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
    private Long price;
    private Integer discountRate;
    private Long discountedPrice;
    private String thumbnailUrl;
}
