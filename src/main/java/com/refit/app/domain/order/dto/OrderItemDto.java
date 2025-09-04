package com.refit.app.domain.order.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDto {
    private Long orderItemId;
    private Long productId;
    private String thumbnailUrl;
    private String brandName;
    private String productName;
    private int remainingCount;
    private int price;
    private Integer discountRate;
    private int discountedPrice;
    private String purchaseDate;
}
