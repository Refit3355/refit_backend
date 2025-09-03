package com.refit.app.domain.memberProduct.dto.response;

import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductRecommendationDto {
    private Long productId;
    private Integer categoryId;
    private String productName;
    private String brandName;
    private Long price;
    private String thumbnailUrl;
    private Integer discountRate;
    private Long discountedPrice;
    private Long stock;
    private Double score;         // 최종 스코어
    private Double baseSimilarity; // SIM_OVERALL
    private Integer rankOrder;     // 원본 이웃 순위
    private List<Long> effectIds;  // 후보 제품의 효과들
}
