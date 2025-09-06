package com.refit.app.domain.payment.dto.response;

import java.util.List;
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

    private String orderCode;
    private String orderName;
    private String method;
    private String firstItemThumb;
    private Integer itemCount;
    private List<ConfirmPaymentItemDto> items;
}