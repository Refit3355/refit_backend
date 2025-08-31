package com.refit.app.domain.payment.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;


@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartialCancelRequest {
    @NotBlank
    private String cancelReason;
    @Min(1)
    private Long cancelAmount;
    private Long taxFreeAmount;
    private String idempotencyKey;
}
