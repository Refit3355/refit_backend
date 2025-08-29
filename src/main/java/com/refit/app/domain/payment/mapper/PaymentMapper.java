package com.refit.app.domain.payment.mapper;

import com.refit.app.domain.payment.dto.OrderItemRowDto;
import com.refit.app.domain.payment.dto.OrderRowDto;
import com.refit.app.domain.payment.dto.PaymentCancelRowDto;
import com.refit.app.domain.payment.dto.PaymentRowDto;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PaymentMapper {

    void insertPayment(PaymentRowDto row);

    PaymentRowDto findPaymentById(@Param("paymentId") Long paymentId);

    PaymentRowDto findPaymentByKey(@Param("paymentKey") String paymentKey);

    void updatePaymentOnApproved(@Param("paymentId") Long paymentId,
            @Param("balanceAmount") Long balanceAmount,
            @Param("status") Integer status,
            @Param("receiptUrl") String receiptUrl,
            @Param("rawJson") String rawJson);

    void updatePaymentStatusAndBalance(@Param("paymentId") Long paymentId,
            @Param("status") Integer status,
            @Param("balanceAmount") Long balanceAmount);

    void insertPaymentCancel(PaymentCancelRowDto row);

    List<PaymentCancelRowDto> listPaymentCancels(@Param("paymentId") Long paymentId);

    OrderRowDto findOrderForUpdate(@Param("orderCode") String orderCode);

    List<OrderItemRowDto> findOrderItems(@Param("orderId") Long orderId);

    void markOrderPaid(@Param("orderId") Long orderId);

    void updateOrderStatus(@Param("orderId") Long orderId, @Param("status") Integer status);

    void increaseCanceledCount(@Param("orderItemId") Long orderItemId,
            @Param("addCount") Integer addCount);

    OrderRowDto findOrderForUpdateByExtId(@Param("orderId") String orderId);

    void updateStatusToApprovedByOrderId(@Param("orderId") Long orderId);
}
