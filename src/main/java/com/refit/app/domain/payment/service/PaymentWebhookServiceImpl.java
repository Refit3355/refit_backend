package com.refit.app.domain.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.refit.app.domain.payment.dto.OrderRowDto;
import com.refit.app.domain.payment.dto.PaymentRowDto;
import com.refit.app.domain.payment.mapper.PaymentMapper;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@Transactional
public class PaymentWebhookServiceImpl implements PaymentWebhookService {

    private final PaymentMapper mapper;
    private final WebClient tossWebClient;
    private final ObjectMapper om;

    public PaymentWebhookServiceImpl(
            PaymentMapper mapper,
            @Qualifier("tossWebClient") WebClient tossWebClient,
            ObjectMapper om
    ) {
        this.mapper = mapper;
        this.tossWebClient = tossWebClient;
        this.om = om;
    }

    public void handle(Map<String,Object> payload) {
        String eventType = String.valueOf(payload.get("eventType")); // PAYMENT_STATUS_CHANGED / DEPOSIT_CALLBACK ...
        Map<String,Object> data = (Map<String,Object>) payload.get("data");

        if ("PAYMENT_STATUS_CHANGED".equals(eventType)) {
            // data에는 Payment 객체 일부가 들어옴: paymentKey, status 등
            String paymentKey = (String) data.get("paymentKey");
            String statusStr  = (String) data.get("status"); // READY/IN_PROGRESS/DONE/CANCELED/...
            PaymentRowDto p = mapper.findPaymentByKey(paymentKey);

            // 없으면(예: 가상계좌 비동기) 필요 시 결제 조회 API로 보강 후 생성
            if (p == null) {
                // 토스 결제 조회 API로 상세 조회 → PAYMENT 행 생성 로직 추가
                return;
            }

            int status = mapToLocalStatus(statusStr);
            Long newBalance = p.getBalanceAmount(); // 조회 응답에 balanceAmount 있다면 갱신

            mapper.updatePaymentStatusAndBalance(p.getPaymentId(), status, newBalance==null? p.getBalanceAmount(): newBalance);
            // 주문 상태 동기화
            int orderStatus = (statusStr.equals("DONE")) ? 1 : (statusStr.equals("CANCELED") ? 3 : p.getStatus());
            mapper.updateOrderStatus(p.getOrderId(), orderStatus);
            return;
        }

        if ("DEPOSIT_CALLBACK".equals(eventType)) {
            // 가상계좌 입금/취소 콜백: orderId, status, secret 포함
            String orderCode = String.valueOf(data.get("orderId"));
            String status  = String.valueOf(data.get("status")); // WAITING_FOR_DEPOSIT / DONE / CANCELED ...
            // secret 검증 필요 시 data.secret vs Payment.virtualAccount.secret 비교 권장(저장해두거나 조회)

            // DONE이면 주문 결제 완료로 마킹
            OrderRowDto order = mapper.findOrderForUpdate(orderCode);
            if (order != null && "DONE".equals(status)) {
                mapper.markOrderPaid(order.getOrderId());
            }
            return;
        }

        // CANCEL_STATUS_CHANGED 등 추가 처리
    }

    private int mapToLocalStatus(String status) {
        // 토스 status → 로컬 status 매핑
        // READY/IN_PROGRESS/DONE/CANCELED/ABORTED/EXPIRED 등
        return switch (status) {
            case "DONE"     -> 1; // APPROVED
            case "CANCELED" -> 3; // CANCELED
            default         -> 0; // REQUESTED/기타
        };
    }
}
