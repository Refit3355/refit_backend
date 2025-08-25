package com.refit.app.domain.product.dto;

import lombok.Getter;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductRecommendationItemDto {
    private Long productId;
    private String thumbnailUrl;
    private String brandName;
    private String productName;
    private Integer discountRate;
    private Long price;
    private Long discountedPrice;
}
