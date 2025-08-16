package com.refit.app.domain.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDto {
    private Long id;
    private String thumbnailUrl;
    private String brandName;
    private String productName;
    private Integer discountRate;
    private int price;
    private int discountedPrice;
    private Integer sales;
}
