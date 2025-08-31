package com.refit.app.domain.payment.mapper;

import com.refit.app.domain.payment.dto.OrderItemRowDto;
import com.refit.app.domain.payment.dto.OrderRowDto;
import com.refit.app.domain.payment.dto.PaymentCancelRowDto;
import com.refit.app.domain.payment.dto.PaymentRowDto;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PaymentMapper {

    // PAYMENT
    void insertPayment(PaymentRowDto row);
    PaymentRowDto findPaymentByKey(@Param("paymentKey") String paymentKey);
    void updatePaymentOnApproved(@Param("paymentId") Long paymentId,
            @Param("balanceAmount") Long balanceAmount,
            @Param("status") Integer status,
            @Param("receiptUrl") String receiptUrl,
            @Param("rawJson") String rawJson);
    void updatePaymentStatusAndBalance(@Param("paymentId") Long paymentId,
            @Param("status") Integer status,
            @Param("balanceAmount") Long balanceAmount);

    // ORDER / ORDER_ITEM
    OrderRowDto findOrderForUpdate(@Param("orderCode") String orderCode);
    List<OrderItemRowDto> findOrderItems(@Param("orderId") Long orderId);
    void markOrderPaid(@Param("orderId") Long orderId);
    void updateOrderStatus(@Param("orderId") Long orderId, @Param("status") Integer status);
    void increaseCanceledCount(@Param("orderItemId") Long orderItemId, @Param("addCount") Integer addCount);
    void updateStatusToApprovedByOrderId(@Param("orderId") Long orderId);

    // 취소 검증/결제 조회
    OrderItemRowDto findOrderItemForCancel(@Param("orderItemId") Long orderItemId);
    PaymentRowDto findActivePaymentByOrderId(@Param("orderId") Long orderId);

    // 과취소/경합 방지 UPDATE
    int conditionalIncreaseCanceledCount(@Param("orderItemId") Long orderItemId, @Param("inc") Integer inc);

    // 집계
    Map<String, Object> aggregateOrderCancelState(@Param("orderId") Long orderId);

    // 멱등/배송비 조정 플래그
    boolean existsPaymentCancelByReqId(@Param("cancelRequestId") String cancelRequestId);
    boolean existsShippingAdjApplied(@Param("paymentId") Long paymentId);

    // 주문 금액/배송비 스냅샷 (잠금)
    OrderRowDto findOrderMoneyForUpdate(@Param("orderId") Long orderId);

    // 취소 이력(배송비 조정 플래그 포함)
    void insertPaymentCancel(@Param("paymentId") Long paymentId,
            @Param("cancelRequestId") String cancelRequestId,
            @Param("cancelAmount") Long cancelAmount,
            @Param("taxFreeAmount") Long taxFreeAmount,
            @Param("cancelReason") String cancelReason,
            @Param("canceledAt") LocalDateTime canceledAt,
            @Param("rawJson") String rawJson,
            @Param("shippingAdjApplied") Integer shippingAdjApplied);
}
