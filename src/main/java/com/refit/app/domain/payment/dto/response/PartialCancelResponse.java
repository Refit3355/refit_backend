package com.refit.app.domain.payment.dto.response;

import lombok.*;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartialCancelResponse {
    private Long paymentId;
    private Long canceledAmount;
    private Long balanceAmount;
    private String status;
    private String canceledAt;
}
