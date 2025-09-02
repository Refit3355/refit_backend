package com.refit.app.infra.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.refit.app.domain.product.dto.response.ProductRecommendationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
@Slf4j
public class RedisRecommendationCacheRepository implements RecommendationCacheRepository {

    private final StringRedisTemplate redis;
    private final ObjectMapper om;

    @Override
    public ProductRecommendationResponse get(String key) {
        try {
            String json = redis.opsForValue().get(key);
            if (json == null) return null;
            return om.readValue(json, new TypeReference<ProductRecommendationResponse>() {});
        } catch (Exception e) {
            return null; // 캐시 장애 시 소프트 페일
        }
    }

    @Override
    public void put(String key, ProductRecommendationResponse value, long ttlSeconds) {
        try {
            String json = om.writeValueAsString(value);
            redis.opsForValue().set(key, json, ttlSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Redis put 실패: key=" + key, e);
        }
    }

}
}
