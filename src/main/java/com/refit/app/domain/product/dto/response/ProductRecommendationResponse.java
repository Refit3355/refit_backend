package com.refit.app.domain.product.dto.response;


import com.refit.app.domain.product.dto.ProductRecommendationItemDto;
import lombok.*;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductRecommendationResponse {
    private List<ProductRecommendationItemDto> items;
}