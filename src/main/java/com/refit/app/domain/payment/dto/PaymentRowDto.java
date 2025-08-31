package com.refit.app.domain.payment.dto;

import java.time.LocalDateTime;
import lombok.*;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRowDto {
    private Long paymentId;
    private Long orderId;
    private String orderCode;
    private String paymentKey; // 토스
    private String method;
    private String currency;
    private Long totalAmount; // 승인 금액
    private Long balanceAmount; // 남은 환불 금액
    private Integer status;
    private LocalDateTime approvedAt;
    private String receiptUrl;
    private String rawJson;
}
