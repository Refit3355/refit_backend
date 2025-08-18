package com.refit.app.infra.cache;

import com.refit.app.domain.product.dto.response.ProductRecommendationResponse;

public interface RecommendationCacheRepository {
    ProductRecommendationResponse get(String key);
    void put(String key, ProductRecommendationResponse value, long ttlSeconds);
}
