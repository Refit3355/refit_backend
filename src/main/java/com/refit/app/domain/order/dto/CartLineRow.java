package com.refit.app.domain.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartLineRow {
    private Long productId;
    private String productName;
    private String brandName;
    private String thumbnailUrl;
    private Long originalPrice;
    private Integer  discountRate;
    private Long discountedPrice;
    private Integer quantity;
}
