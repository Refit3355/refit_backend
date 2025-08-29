package com.refit.app.domain.payment.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentDetailResponse {
    private Long paymentId;
    private Long orderId;
    private String paymentKey;
    private String method;
    private String currency;
    private Long totalAmount;
    private Long balanceAmount;    // 환불 가능 잔액
    private String status;
    private LocalDateTime approvedAt;
    private String receiptUrl;
    private List<PaymentCancelHistoryResponse> cancels;
}
