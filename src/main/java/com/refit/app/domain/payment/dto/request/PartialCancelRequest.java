package com.refit.app.domain.payment.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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

    private RefundReceiveAccount refundReceiveAccount;

    @Getter @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RefundReceiveAccount {
        // 은행코드: 'KB','HANA','IBK' 같은 영문코드 또는 '004','081' 같은 숫자3자리
        @NotBlank @Size(min = 2, max = 10)
        private String bankCode;

        // 하이픈/공백 제거된 계좌번호
        @NotBlank @Size(min = 6, max = 32)
        private String accountNumber;

        // 예금주명
        @NotBlank @Size(min = 1, max = 50)
        private String holderName;
    }
}
