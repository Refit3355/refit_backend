package com.refit.app.domain.product.dto;

import lombok.Getter;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiRecommendResultItemDto {
    private Integer productId;
    private String name;
    private String category;
    private Integer categoryId;
    private Double sim;
    private Double effMatch;
    private Double finalScore;
    private Integer price;
    private String brand;
    private Integer stock;
    private Integer discountRate;
    private String thumbnailUrl;
    private Double unitsSold;
    private Double ordersSold;
}
