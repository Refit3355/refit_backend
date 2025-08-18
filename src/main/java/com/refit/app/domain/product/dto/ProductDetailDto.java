package com.refit.app.domain.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDetailDto {
    private Long id;
    private String brandName;
    private String productName;
    private String thumbnailUrl;
    private Integer discountRate;
    private Integer price;
    private Integer discountedPrice;
    private String recommendedPeriod;
}
