package com.refit.app.domain.payment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfirmPaymentRequest {
    @NotBlank
    private String paymentKey; // 토스 결제키 (successUrl 파라미터)

    @NotBlank
    private String orderId; // 문자열 주문코드 ORDER_CODE (successUrl 파라미터)

    @NotNull @Positive
    private Long amount;     //결제 금액

    private String method;
}
