package com.refit.app.domain.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Service
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final WebClient tossWebClient;    // TossClientConfig 에서 BasicAuth(secretKey:) 세팅
    private final PaymentMapper paymentMapper;
    private final ProductMapper productMapper;
    private final ObjectMapper om;

    @Value("${toss.api-base}") String tossBase;

    public PaymentServiceImpl(
            @Qualifier("tossWebClient") WebClient tossWebClient,
            PaymentMapper paymentMapper,
            ObjectMapper objectMapper,
            ProductMapper productMapper
    ) {
        this.tossWebClient = tossWebClient;
        this.paymentMapper = paymentMapper;
        this.om = objectMapper;
        this.productMapper = productMapper;
    }

    @Override
    public ConfirmPaymentResponse confirm(ConfirmPaymentRequest req) {
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
    public PartialCancelResponse partialCancel(Long paymentId, PartialCancelRequest req) {
        PaymentRowDto pay = paymentMapper.findPaymentById(paymentId);
        if (pay == null) throw new RefitException(ErrorCode.ENTITY_NOT_FOUND, "해당 결제 정보 없음");

        long cancelAmount = (req.getCancelAmount() != null)
                ? req.getCancelAmount()
                : calcFromItems(pay.getOrderId(), req.getItems());

        if (cancelAmount <= 0 || cancelAmount > pay.getBalanceAmount())
            throw new RefitException(ErrorCode.INVALID_CANCEL_AMOUNT, "부적절한 취소 금액");

        String idemp = (req.getIdempotencyKey() != null)
                ? req.getIdempotencyKey()
                : UUID.randomUUID().toString();

        Map<String,Object> body = new HashMap<>();
        body.put("cancelReason", req.getCancelReason());
        body.put("cancelAmount", cancelAmount);
        if (req.getTaxFreeAmount()!=null) body.put("taxFreeAmount", req.getTaxFreeAmount());

        Map<?,?> cancelObj = tossWebClient.post()
                .uri("/v1/payments/{paymentKey}/cancel", pay.getPaymentKey())
                .header("Idempotency-Key", idemp) // 토스 멱등키 지원
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        Number newBalance = (Number) cancelObj.get("balanceAmount");
        long balance = newBalance.longValue();

        // 품목별 수량 취소 반영
        if (req.getItems()!=null) {
            for (PartialCancelItem it : req.getItems()) {
                paymentMapper.increaseCanceledCount(it.getOrderItemId(), it.getCancelCount());
            }
        }

        // 취소 이력 기록
        PaymentCancelRowDto cancelRow = PaymentCancelRowDto.builder()
                .paymentId(paymentId)
                .cancelRequestId(idemp)
                .cancelAmount(cancelAmount)
                .taxFreeAmount(req.getTaxFreeAmount()==null?0:req.getTaxFreeAmount())
                .cancelReason(req.getCancelReason())
                .rawJson(toJson(cancelObj))
                .build();
        paymentMapper.insertPaymentCancel(cancelRow);

        // 결제/주문 상태 전이
        int status = (balance == 0) ? 3 : 2; // 3=CANCELED, 2=PARTIAL_CANCELED
        paymentMapper.updatePaymentStatusAndBalance(paymentId, status, balance);
        paymentMapper.updateOrderStatus(pay.getOrderId(), status);

        return PartialCancelResponse.builder()
                .paymentId(paymentId)
                .canceledAmount(cancelAmount)
                .balanceAmount(balance)
                .status((status==3)?"CANCELED":"PARTIAL_CANCELED")
                .canceledAt(LocalDateTime.now().toString())
                .build();
    }

    private long calcFromItems(Long orderId, List<PartialCancelItem> items) {
        if (items == null || items.isEmpty()) return 0L;
        // 단가 * 취소수량 단순합
        List<OrderItemRowDto> rows = paymentMapper.findOrderItems(orderId);
        Map<Long, OrderItemRowDto> byId = new HashMap<>();
        for (OrderItemRowDto r : rows) byId.put(r.getOrderItemId(), r);

        long sum = 0L;
        for (PartialCancelItem it : items) {
            OrderItemRowDto r = byId.get(it.getOrderItemId());
            if (r == null) throw new RefitException(ErrorCode.ENTITY_NOT_FOUND, "해당 주문 아이템 없음");
            int remain = r.getItemCount() - (r.getCanceledCount()==null?0:r.getCanceledCount());
            if (it.getCancelCount() > remain) throw new RefitException(ErrorCode.INVALID_CANCEL_AMOUNT, "부적절한 취소 개수");
            sum += r.getItemPrice() * it.getCancelCount();
        }
        return sum;
    }

    private String toJson(Object src) {
        try { return om.writeValueAsString(src); } catch (Exception e) { return null; }
    }
}
