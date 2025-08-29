package com.refit.app.domain.payment.dto.response;

import lombok.*;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfirmPaymentResponse {
    private Long paymentId;
    private String paymentKey;
    private Long totalAmount;
    private String status;
    private String receiptUrl;
    private Long orderPk;
}
