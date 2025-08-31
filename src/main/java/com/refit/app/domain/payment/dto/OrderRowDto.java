package com.refit.app.domain.payment.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderRowDto {
    private Long orderId;
    private String orderCode;
    private Long totalPrice;
    private Integer orderStatus;
    private Long goodsAmount;
    private Long deliveryFee;
}