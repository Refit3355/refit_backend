package com.refit.app.domain.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.refit.app.domain.notification.service.NotificationTriggerService;
import com.refit.app.domain.payment.dto.OrderItemRowDto;
import com.refit.app.domain.payment.dto.OrderRowDto;
import com.refit.app.domain.payment.dto.PaymentCancelRowDto;
import com.refit.app.domain.payment.dto.request.PartialCancelItem;
import com.refit.app.domain.payment.dto.PaymentRowDto;
import com.refit.app.domain.payment.dto.request.ConfirmPaymentRequest;
import com.refit.app.domain.payment.dto.request.PartialCancelRequest;
import com.refit.app.domain.payment.dto.response.ConfirmPaymentResponse;
import com.refit.app.domain.payment.dto.response.PartialCancelResponse;
import com.refit.app.domain.payment.mapper.PaymentMapper;
import com.refit.app.domain.product.mapper.ProductMapper;
import com.refit.app.global.exception.ErrorCode;
import com.refit.app.global.exception.RefitException;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Slf4j
@Service
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final WebClient tossWebClient;    // TossClientConfig 에서 BasicAuth(secretKey:) 세팅
    private final PaymentMapper paymentMapper;
    private final ProductMapper productMapper;
    private final ObjectMapper om;

    private final PaymentCancelLogService cancelLogService;
    private final NotificationTriggerService notificationTriggerService;

    @Value("${toss.api-base}") String tossBase;
    private static final long FREE_SHIPPING_THRESHOLD = 30_000L;
    private static final long BASE_DELIVERY_FEE = 3_000L;

    public PaymentServiceImpl(
            @Qualifier("tossWebClient") WebClient tossWebClient,
            PaymentMapper paymentMapper,
            ObjectMapper objectMapper,
            ProductMapper productMapper,
            PaymentCancelLogService cancelLogService,
            NotificationTriggerService notificationTriggerService
    ) {
        this.tossWebClient = tossWebClient;
        this.paymentMapper = paymentMapper;
        this.om = objectMapper;
        this.productMapper = productMapper;
        this.cancelLogService = cancelLogService;
        this.notificationTriggerService = notificationTriggerService;
    }

    @Override
    public ConfirmPaymentResponse confirm(ConfirmPaymentRequest req, Long memberId) {
        // 1) 서버 금액 검증
        String orderCode = req.getOrderId();
        OrderRowDto order = paymentMapper.findOrderForUpdate(orderCode);
        Long orderId = order.getOrderId();
        if (order == null) throw new RefitException(ErrorCode.ENTITY_NOT_FOUND, "해당 주문 없음");
        if (!Objects.equals(order.getTotalPrice(), req.getAmount()))
            throw new RefitException(ErrorCode.ORDER_AMOUNT_MISMATCH, "금액 불일치"); // successUrl 금액과 서버 금액 비교

        // 2) 승인 API 호출
        Map<String,Object> body = Map.of(
                "paymentKey", req.getPaymentKey(),
                "orderId", orderCode,
                "amount", req.getAmount()
        );

        Map<?,?> paymentObj = tossWebClient.post()
                .uri("/v1/payments/confirm") // toss
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .block(); // 간단화를 위해 block

        // 3) 응답 파싱
        String paymentKey = (String) paymentObj.get("paymentKey");
        String method     = (String) paymentObj.get("method");
        String currency   = (String) paymentObj.get("currency");
        Number totalAmount= (Number) paymentObj.get("totalAmount");
        Number balanceAmt = (Number) paymentObj.get("balanceAmount");
        String receiptUrl = null;
        Object receiptObj = paymentObj.get("receipt");
        if (receiptObj instanceof Map<?,?> r) {
            receiptUrl = (String) r.get("url");
        }

        // 4) DB 반영
        PaymentRowDto row = PaymentRowDto.builder()
                .orderId(orderId)
                .orderCode(orderCode)
                .paymentKey(paymentKey)
                .method(method)
                .currency(currency)
                .totalAmount(totalAmount.longValue())
                .balanceAmount(balanceAmt.longValue())
                .status(1) // APPROVED
                .build();
        paymentMapper.insertPayment(row);

        paymentMapper.markOrderPaid(orderId);
        paymentMapper.updateStatusToApprovedByOrderId(orderId);

        // 승인 원문 보관 + 영수증
        paymentMapper.updatePaymentOnApproved(row.getPaymentId(),
                balanceAmt.longValue(), 1, receiptUrl, toJson(paymentObj));

        // product 재고 차감
        List<OrderItemRowDto> orderItems = paymentMapper.findOrderItems(orderId);
        // 교착 방지: 항상 같은 순서로 잠금
        orderItems.sort(Comparator.comparing(OrderItemRowDto::getProductId));

        for (OrderItemRowDto it : orderItems) {
            Long productId = it.getProductId();
            int qty = it.getItemCount();
            if (qty <= 0) {
                throw new RefitException(ErrorCode.OUT_OF_STOCK, "잘못된 수량: " + qty);
            }

            // 재고 행 잠금 + 현재 재고 확인
            Integer stock = productMapper.selectStockForUpdate(productId);
            if (stock == null) {
                throw new RefitException(ErrorCode.ENTITY_NOT_FOUND, "상품 없음: " + productId);
            }
            if (stock < qty) {
                throw new RefitException(ErrorCode.OUT_OF_STOCK,
                        "재고 부족: productId=" + productId + ", stock=" + stock + ", need=" + qty);
            }

            int updated = productMapper.decreaseStock(productId, qty);

            // 배치인 경우 -2(SUCCESS_NO_INFO) 가능
            log.info("PRODUCT 재고 UPDATE 결과:" + updated);
            if (updated == 0) {
                throw new RefitException(ErrorCode.OUT_OF_STOCK,
                        "차감 실패(동시성): productId=" + productId + ", qty=" + qty);
            }
        }

        notificationTriggerService.notifyPaymentCompleted(memberId, orderId,
                order.getOrderSummary() + "의 결제가 완료되었습니다.");


        return ConfirmPaymentResponse.builder()
                .paymentId(row.getPaymentId())
                .paymentKey(paymentKey)
                .totalAmount(totalAmount.longValue())
                .status("APPROVED")
                .receiptUrl(receiptUrl)
                .orderPk(orderId)
                .build();
    }

    @Override
    @Transactional
    public PartialCancelResponse partialCancel(Long orderItemId, PartialCancelRequest req, Long memberId) {
        // 0) 입력 검증
        Long reqAmt = req.getCancelAmount();
        if (reqAmt == null || reqAmt < 1) {
            throw new RefitException(ErrorCode.INVALID_CANCEL_AMOUNT, "취소 금액이 올바르지 않습니다.");
        }

        // 1) 아이템 조회/기초 검증
        OrderItemRowDto item = paymentMapper.findOrderItemForCancel(orderItemId);
        if (item == null) throw new RefitException(ErrorCode.ENTITY_NOT_FOUND, "주문 아이템을 찾을 수 없습니다.");

        int canceled = item.getCanceledCount() == null ? 0 : item.getCanceledCount();
        int remain   = item.getItemCount() - canceled;
        long unit    = item.getItemPrice();
        long cancelAmount = reqAmt;

        if (unit <= 0) throw new RefitException(ErrorCode.INVALID_CANCEL_AMOUNT, "아이템 단가가 올바르지 않습니다.");
        if (cancelAmount % unit != 0) throw new RefitException(ErrorCode.INVALID_CANCEL_AMOUNT, "취소 금액은 단가의 배수여야 합니다.");
        int cancelCount = (int)(cancelAmount / unit);
        if (cancelCount < 1 || cancelCount > remain) {
            throw new RefitException(ErrorCode.INVALID_CANCEL_AMOUNT, "취소 가능 수량을 초과했습니다.");
        }

        // 2) 결제 조회/잔액 검증
        PaymentRowDto pay = paymentMapper.findActivePaymentByOrderId(item.getOrderId());
        if (pay == null) throw new RefitException(ErrorCode.ENTITY_NOT_FOUND, "결제 정보를 찾을 수 없습니다.");
        if (cancelAmount > pay.getBalanceAmount())
            throw new RefitException(ErrorCode.INVALID_CANCEL_AMOUNT, "취소 금액이 결제 잔액을 초과합니다.");

        // 2-1) 주문 스냅샷 잠금 (배송비 조정의 경합 직렬화)
        OrderRowDto moneyLocked = paymentMapper.findOrderMoneyForUpdate(item.getOrderId());

        // 3) 멱등키
        final String idemp = (req.getIdempotencyKey()!=null && !req.getIdempotencyKey().isBlank())
                ? req.getIdempotencyKey() : UUID.randomUUID().toString();

        // 4) 배송비 정책 계산(잠금 하)
        ShippingAdjustment adj = computeShippingAdjustmentLocked(
                moneyLocked, pay.getPaymentId(), item.getOrderId(),
                orderItemId, cancelCount, cancelAmount);

        long pgCancelAmount = adj.pgCancelAmount;
        if (pgCancelAmount > pay.getBalanceAmount())
            throw new RefitException(ErrorCode.INVALID_CANCEL_AMOUNT, "환불 금액이 결제 잔액을 초과합니다.");

        // 5) PG(토스) 호출
        Map<String, Object> body = new HashMap<>();
        body.put("cancelReason", req.getCancelReason());
        body.put("cancelAmount", pgCancelAmount);
        if (req.getTaxFreeAmount()!=null) body.put("taxFreeAmount", req.getTaxFreeAmount());

        Map<?,?> cancelObj;
        long balance = pay.getBalanceAmount();
        if (pgCancelAmount > 0) {
            try {
                cancelObj = tossWebClient.post()
                        .uri("/v1/payments/{paymentKey}/cancel", pay.getPaymentKey())
                        .header("Idempotency-Key", idemp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(body)
                        .retrieve()
                        .bodyToMono(Map.class)
                        .block();
            } catch (WebClientResponseException e) {
                log.error("Toss cancel HTTP error: status={}, body={}",
                        e.getRawStatusCode(), e.getResponseBodyAsString(), e);
                throw new RefitException(ErrorCode.INTERNAL_SERVER_ERROR, "PG 결제 취소 호출 실패");
            }
            if (!(cancelObj.get("balanceAmount") instanceof Number n)) {
                throw new RefitException(ErrorCode.INTERNAL_SERVER_ERROR, "PG 응답 이상(잔액 누락)");
            }
            balance = n.longValue();
        } else {
            cancelObj = Map.of("message", "shipping adjustment only, no PG refund");
        }

        // 6) PG 성공 로그(REQUIRES_NEW) — 배송비 조정 중복 방지 플래그 포함
        cancelLogService.logCancel(
                pay.getPaymentId(),
                idemp,
                pgCancelAmount,
                (req.getTaxFreeAmount()==null?0L:req.getTaxFreeAmount()),
                req.getCancelReason(),
                LocalDateTime.now(),
                toJson(cancelObj),
                adj.shippingAdjApplied
        );

        // 7) 과취소/경합 방지: 조건부 증가
        int updated = paymentMapper.conditionalIncreaseCanceledCount(orderItemId, cancelCount);
        if (updated <= 0) {
            // 경합/멱등 허용 — 정말 문제있는 경우만 예외
            boolean logged = paymentMapper.existsPaymentCancelByReqId(idemp);
            OrderItemRowDto fresh = paymentMapper.findOrderItemForCancel(orderItemId);
            int freshCanceled = fresh.getCanceledCount() == null ? 0 : fresh.getCanceledCount();
            int freshRemain   = fresh.getItemCount() - freshCanceled;
            if (!logged && freshRemain >= remain) {
                throw new RefitException(ErrorCode.INVALID_CANCEL_AMOUNT, "이미 취소되었거나 취소 가능 수량을 초과했습니다.");
            }
            log.warn("No rows updated (race or already applied). orderItemId={}, beforeRemain={}, afterRemain={}",
                    orderItemId, remain, freshRemain);
        }

        // 8) 결제/주문 상태 전이 (PG 응답 기준)
        int pStatus = (balance == 0) ? 3 : 2; // 3=CANCELED, 2=PARTIAL_CANCELED
        if (pgCancelAmount > 0) {
            paymentMapper.updatePaymentStatusAndBalance(pay.getPaymentId(), pStatus, balance);
        }

        Map<String, Object> agg = paymentMapper.aggregateOrderCancelState(item.getOrderId());
        int fullCanceled = ((Number)(agg.get("FULL_CANCELED")==null?0:agg.get("FULL_CANCELED"))).intValue();
        int total        = ((Number)(agg.get("TOTAL")==null?0:agg.get("TOTAL"))).intValue();
        if (total > 0) {
            if (fullCanceled == total) paymentMapper.updateOrderStatus(item.getOrderId(), 3);
            else if (fullCanceled > 0) paymentMapper.updateOrderStatus(item.getOrderId(), 2);
        }

        notificationTriggerService.notifyPaymentCanceled(memberId, moneyLocked.getOrderId(),
                moneyLocked.getOrderSummary() + "의 결제가 취소되었습니다.");

        // 응답은 실제 PG 환불금액으로 반환
        return PartialCancelResponse.builder()
                .paymentId(pay.getPaymentId())
                .canceledAmount(pgCancelAmount)
                .balanceAmount(balance)
                .status((pStatus == 3) ? "CANCELED" : "PARTIAL_CANCELED")
                .canceledAt(LocalDateTime.now().toString())
                .build();
    }



    private String toJson(Object src) {
        try { return om.writeValueAsString(src); } catch (Exception e) { return null; }
    }

    private static class ShippingAdjustment {
        long pgCancelAmount;     // PG로 보내는 실제 환불금
        int  shippingAdjApplied; // 1=배송비 조정(차감/환불) 했음
        long delta;              // (+)배송비 환불 / (-)배송비 차감
    }

    private ShippingAdjustment computeShippingAdjustmentLocked(
            OrderRowDto moneyLocked, Long paymentId, Long orderId,
            Long orderItemId, int cancelCount, long cancelAmount) {

        ShippingAdjustment r = new ShippingAdjustment();
        r.pgCancelAmount = cancelAmount;
        r.delta = 0;
        r.shippingAdjApplied = 0;

        long goodsAmount = moneyLocked.getGoodsAmount()==null?0L:moneyLocked.getGoodsAmount();
        long deliveryFee = moneyLocked.getDeliveryFee()==null?0L:moneyLocked.getDeliveryFee();
        int  orderStatus = moneyLocked.getOrderStatus()==null?0:moneyLocked.getOrderStatus();

        // 이미 배송비 조정 기록 있으면 중복 방지
        if (paymentMapper.existsShippingAdjApplied(paymentId)) return r;

        // 현재까지 취소된 금액 + 이번 취소분 가정
        long canceledSoFar = 0L;
        for (OrderItemRowDto it : paymentMapper.findOrderItems(orderId)) {
            int cc = it.getCanceledCount()==null?0:it.getCanceledCount();
            canceledSoFar += it.getItemPrice() * cc;
            if (Objects.equals(it.getOrderItemId(), orderItemId)) {
                canceledSoFar += it.getItemPrice() * cancelCount;
            }
        }
        long remainAfter = Math.max(0L, goodsAmount - canceledSoFar);

        // 규칙 1) 무료배송 주문이 임계(3만원) 아래로 최초 하락 → 3,000원 차감
        if (deliveryFee == 0 && goodsAmount >= FREE_SHIPPING_THRESHOLD && remainAfter < FREE_SHIPPING_THRESHOLD) {
            r.delta = -BASE_DELIVERY_FEE;
            r.pgCancelAmount = Math.max(0L, cancelAmount + r.delta);
            r.shippingAdjApplied = 1;
            return r;
        }

        // 규칙 2) 유료배송 & 전액환불 & 배송 전(상태<5) → 배송비 환불
        if (deliveryFee > 0 && remainAfter == 0 && orderStatus < 5) {
            r.delta = deliveryFee;
            r.pgCancelAmount = cancelAmount + r.delta;
            r.shippingAdjApplied = 1;
            return r;
        }

        return r;
    }

}
