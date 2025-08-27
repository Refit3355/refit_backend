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
    private String brandName;
    private String productName;
    private Long price;            // 원가
    private Integer discountRate;  // 할인율 (%)
    private String thumbnailUrl;
    private Long discountedPrice;  // 할인 적용가 (100원 단위 절사)
}