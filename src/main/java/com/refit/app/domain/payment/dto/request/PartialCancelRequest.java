package com.refit.app.domain.payment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.util.List;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartialCancelRequest {
    @NotBlank
    private String cancelReason;
    @Positive
    private Long cancelAmount;
    private Long taxFreeAmount;
    private List<PartialCancelItem> items;
    private String idempotencyKey;
}
