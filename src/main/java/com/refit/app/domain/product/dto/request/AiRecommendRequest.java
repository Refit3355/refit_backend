package com.refit.app.domain.product.dto.request;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiRecommendRequest {
    private Long memberId;
    private Integer productType;
    private Integer preferCategoryId;
    private String  location;
    private Integer topk;

    @JsonProperty("final")
    private Integer finalCount;
}