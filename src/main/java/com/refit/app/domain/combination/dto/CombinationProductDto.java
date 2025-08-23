package com.refit.app.domain.combination.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CombinationProductDto {
    private Long productId;
    private String productName;
    private String brandName;
    private int price;
    private int discountRate;
    private int discountedPrice;
    private String thumbnailUrl;
}
