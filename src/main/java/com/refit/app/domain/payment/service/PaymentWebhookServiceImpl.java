package com.refit.app.domain.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.refit.app.domain.notification.service.NotificationTriggerService;
import com.refit.app.domain.payment.dto.OrderRowDto;
import com.refit.app.domain.payment.dto.PaymentRowDto;
import com.refit.app.domain.payment.mapper.PaymentMapper;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Service
@Transactional
public class PaymentWebhookServiceImpl implements PaymentWebhookService {

    private final PaymentMapper mapper;
    private final WebClient tossWebClient;
    private final ObjectMapper om;
    private final NotificationTriggerService notificationTriggerService;

    public PaymentWebhookServiceImpl(
            PaymentMapper mapper,
            @Qualifier("tossWebClient") WebClient tossWebClient,
            ObjectMapper om,
            NotificationTriggerService notificationTriggerService
    ) {
        this.mapper = mapper;
        this.tossWebClient = tossWebClient;
        this.om = om;
        this.notificationTriggerService = notificationTriggerService;
    }

    public void handle(Map<String,Object> payload) {
        final String eventType = String.valueOf(payload.get("eventType")); // 없으면 "null"
        if (payload.containsKey("eventType")) {
            handleStatusChanged(payload);
        } else {
            handleDepositCallback(payload); // eventType 없는 경우
        }
    }

    public void handleStatusChanged(Map<String,Object> payload) {
        String eventType = String.valueOf(payload.get("eventType")); // PAYMENT_STATUS_CHANGED / DEPOSIT_CALLBACK ...
        Map<String,Object> data = (Map<String,Object>) payload.get("data");

        String status = String.valueOf(data.get("status")); // READY / IN_PROGRESS / DONE / CANCELED / EXPIRED
        String methodRaw = String.valueOf(data.get("method"));
        boolean isVA = isVirtualAccount(methodRaw) || data.get("virtualAccount") != null;

        String paymentKey = String.valueOf(data.get("paymentKey"));
        PaymentRowDto p = mapper.findPaymentByKey(paymentKey);

        // 결제 레코드가 없으면 보강(방어)
        if (p == null) {
            Map<String, Object> pay = tossWebClient.get()
                    .uri("/v1/payments/{paymentKey}", paymentKey)
                    .retrieve().bodyToMono(Map.class).block();
            String orderIdStr = String.valueOf(pay.get("orderId"));
            OrderRowDto order = mapper.findOrderForUpdate(orderIdStr);
            p = PaymentRowDto.builder()
                    .orderId(order.getOrderId())
                    .orderCode(orderIdStr)
                    .paymentKey(paymentKey)
                    .method(String.valueOf(pay.get("method")))
                    .totalAmount(((Number)pay.get("totalAmount")).longValue())
                    .balanceAmount(((Number)pay.get("balanceAmount")).longValue())
                    .status(0)
                    .build();
            mapper.insertPayment(p);
        }

        // 상태 반영 (아이템도 함께)
        if ("DONE".equals(status)) {
            mapper.updatePaymentStatusAndBalance(p.getPaymentId(), 1, p.getBalanceAmount());
            mapper.updateOrderStatus(p.getOrderId(), 1);  // 결제완료
            mapper.updateOrderItemsStatusByOrderId(p.getOrderId(), 1);

            // 가상계좌만 푸시 (멱등: 이미 완료면 재발송 안 함)
            OrderRowDto ord = mapper.findOrderForUpdate(p.getOrderCode());
            if (isVA && ord.getOrderStatus() != null && ord.getOrderStatus() == 1) {
                Long memberId = mapper.findMemberIdByOrderId(p.getOrderId());
                try {
                    notificationTriggerService.notifyPaymentCompleted(
                            memberId, p.getOrderId(), ord.getOrderSummary() + "의 입금이 완료되었습니다."
                    );
                } catch (Exception e) {
                    log.warn("push failed: orderId={}", p.getOrderId(), e);
                }
            }
        } else if ("CANCELED".equals(status) || "EXPIRED".equals(status)) {
            mapper.updatePaymentStatusAndBalance(p.getPaymentId(), 3, p.getBalanceAmount());
            mapper.updateOrderStatus(p.getOrderId(), 3);
            mapper.updateOrderItemsStatusByOrderId(p.getOrderId(), 3);
        } else if ("READY".equals(status) || "WAITING_FOR_DEPOSIT".equals(status) || "IN_PROGRESS".equals(status)) {
            if (isVA) {
                mapper.updateOrderStatus(p.getOrderId(), 12); // 입금대기
                mapper.updateOrderItemsStatusByOrderId(p.getOrderId(), 12);
            }
        }
    }

    private void handleDepositCallback(Map<String,Object> payload) {
        // Toss VA 입금 콜백: eventType 없음
        // {createdAt, secret, orderId, status, transactionKey}
        String orderCode = String.valueOf(payload.get("orderId"));
        String status    = String.valueOf(payload.get("status"));
        String secret    = String.valueOf(payload.get("secret"));

        OrderRowDto order = mapper.findOrderForUpdate(orderCode);
        if (order == null) return;

        // 시크릿 검증 (최근 결제의 VA_SECRET 과 비교)
        PaymentRowDto active = mapper.findActivePaymentByOrderId(order.getOrderId());
        if (active == null || secret == null) {
            log.warn("VA deposit callback missing payment/secret. order={}", orderCode);
            return;
        }

        if ("DONE".equals(status)) {
            // 멱등 가드: 이미 결제완료면 무시
            if (order.getOrderStatus() != null && order.getOrderStatus() == 1) return;

            mapper.markOrderPaid(order.getOrderId()); // ORDERS=1
            mapper.updateStatusToApprovedByOrderId(order.getOrderId()); // ORDER_ITEM=1

            Long memberId = mapper.findMemberIdByOrderId(order.getOrderId());
            try {
                notificationTriggerService.notifyPaymentCompleted(
                        memberId, order.getOrderId(), order.getOrderSummary() + "의 입금이 완료되었습니다."
                );
            } catch (Exception e) {
                log.warn("push failed: orderId={}", order.getOrderId(), e);
            }
        } else if ("WAITING_FOR_DEPOSIT".equals(status)) {
            mapper.updateOrderStatus(order.getOrderId(), 12);
            mapper.updateOrderItemsStatusByOrderId(order.getOrderId(), 12);
        } else if ("CANCELED".equals(status) || "EXPIRED".equals(status)) {
            mapper.updateOrderStatus(order.getOrderId(), 3);
            mapper.updateOrderItemsStatusByOrderId(order.getOrderId(), 3);
        }
    }

    private boolean isVirtualAccount(String methodRaw) {
        if (methodRaw == null) return false;
        String m = methodRaw.trim().toUpperCase();
        return "VIRTUAL_ACCOUNT".equals(m) || "가상계좌".equals(methodRaw.trim());
    }
}
