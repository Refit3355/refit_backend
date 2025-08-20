package com.refit.app.domain.order.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OrderItemDto {
    private Long orderItemId;
    private Long productId;
    private String thumbnailUrl;
    private String brandName;
    private String productName;
    private int itemCount;
    private String price;
    private Integer discountRate;
    private int discountedPrice;
    private String purchaseDate;
}
