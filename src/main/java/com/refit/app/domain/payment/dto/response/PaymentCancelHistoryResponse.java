package com.refit.app.domain.payment.dto.response;

import lombok.*;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentCancelHistoryResponse {
    private Long paymentCancelId;
    private Long paymentId;
    private String cancelRequestId;
    private Long cancelAmount;
    private Long taxFreeAmount;
    private String cancelReason;
    private String canceledAt;
}
