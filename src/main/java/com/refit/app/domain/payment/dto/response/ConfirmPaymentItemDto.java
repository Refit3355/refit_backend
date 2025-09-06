package com.refit.app.domain.payment.dto.response;

import lombok.*;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfirmPaymentItemDto {
    private Long productId;
    private String brandName;
    private String productName;
    private Long price;
    private Long originalPrice;
    private Integer quantity;
    private String thumbnailUrl;
}