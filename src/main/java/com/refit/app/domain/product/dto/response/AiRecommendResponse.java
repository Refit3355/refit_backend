package com.refit.app.domain.product.dto.response;

import com.refit.app.domain.product.dto.AiRecommendResultItemDto;
import lombok.Getter;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiRecommendResponse {
    private Map<String, Object> weather;
    private String query;
    private List<String> targetEffects;
    private List<AiRecommendResultItemDto> results;
}
