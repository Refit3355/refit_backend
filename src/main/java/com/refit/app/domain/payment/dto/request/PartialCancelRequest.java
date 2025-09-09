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

    // 토스 은행 코드 (예: "088" 신한, "090" 카카오뱅크 등 — 테스트 시 아무 유효코드)
    private String refundBankCode;
    private String refundAccountNumber;  // 하이픈 제거된 숫자계좌
    private String refundHolderName; // 예금주명
}
