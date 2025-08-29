package com.refit.app.domain.payment.dto;

import java.time.LocalDateTime;
import lombok.*;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentCancelRowDto {
    private Long paymentCancelId;
    private Long paymentId;
    private String cancelRequestId; // 멱등키
    private Long cancelAmount;
    private Long taxFreeAmount;
    private String cancelReason;
    private LocalDateTime canceledAt;
    private String rawJson;
}
