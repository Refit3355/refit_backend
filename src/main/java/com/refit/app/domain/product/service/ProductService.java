package com.refit.app.domain.product.service;

import com.refit.app.domain.product.dto.response.ProductDetailResponse;
import com.refit.app.domain.product.dto.response.ProductListResponse;
import com.refit.app.domain.product.dto.response.ProductRecommendationResponse;
import com.refit.app.domain.product.dto.response.ProductSuggestResponse;
import com.refit.app.domain.product.model.SortType;
import java.util.List;

public interface ProductService {

    ProductListResponse getProducts(Integer categoryId, String group, SortType sortType, String cursor, int limit);

    ProductDetailResponse getProductDetail(Long id);

    ProductListResponse searchProductsByName(String q, SortType sort, int limit, String cursor);

    ProductSuggestResponse suggestProducts(String keyword, int limit, String cursor);

    ProductListResponse getLikedProducts(List<Long> likedItems);

    /**
     * @param productType 0:전체, 1:뷰티, 2:헤어, 3:건기능
     * @param limit       기본 10 (1~200 권장)
     * @param memberId    JWT에서 추출 (캐시미스 시 필수)
     */
    ProductRecommendationResponse getRecommendations(
            int productType, int limit, Long memberId
    );
}
