package com.refit.app.domain.product.service;

import com.refit.app.domain.product.dto.ImageDto;
import com.refit.app.domain.product.dto.ProductDetailDto;
import com.refit.app.domain.product.dto.ProductSimpleDto;
import com.refit.app.domain.product.dto.response.ProductDetailResponse;
import com.refit.app.domain.product.dto.response.ProductListResponse;
import com.refit.app.domain.product.dto.ProductDto;
import com.refit.app.domain.product.dto.response.ProductRecommendationResponse;
import com.refit.app.domain.product.dto.response.ProductSuggestResponse;
import com.refit.app.domain.product.model.SortType;
import com.refit.app.domain.product.mapper.ProductMapper;
import com.refit.app.global.exception.ErrorCode;
import com.refit.app.global.exception.RefitException;
import com.refit.app.global.util.CursorUtil;
import com.refit.app.infra.ai.AiRecommendClient;
import com.refit.app.infra.cache.RecommendationCacheKey;
import com.refit.app.infra.cache.RecommendationCacheRepository;
import java.util.Collections;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.refit.app.domain.product.dto.request.AiRecommendRequest;
import com.refit.app.domain.product.dto.response.AiRecommendResponse;
import com.refit.app.domain.product.dto.ProductRecommendationItemDto;

import java.util.List;
import java.util.Map;
import org.springframework.util.CollectionUtils;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductMapper productMapper;
    private final RecommendationCacheRepository cacheRepo;
    private final AiRecommendClient aiClient;

    @Value("${external.ai.cache-ttl-sec:20000}")
    private long cacheTtlSec;

    @Override
    public ProductListResponse getProducts(Integer categoryId, String group, SortType sortType, String cursor, int limit) {
        if (limit <= 0 || limit > 100) limit = 20;

        // 커서 디코딩
        var c = CursorUtil.decode(cursor);
        Long lastId       = CursorUtil.asLong(c.get("id"));
        Integer lastPrice = CursorUtil.asInt(c.get("price"));
        Integer lastSales = CursorUtil.asInt(c.get("sales"));

        // 그룹 범위 계산 (categoryId 없을 때만 사용)
        Integer catFrom = null, catTo = null;
        if (categoryId == null && group != null) {
            switch (group.toLowerCase()) {
                case "beauty" -> { catFrom = 0;  catTo = 7; }
                case "health" -> { catFrom = 8;  catTo = 11; }
                default -> { /* 잘못된 값이면 전체 */ }
            }
        }

        int totalCount = productMapper.countProducts(categoryId, catFrom, catTo);

        // 정렬별 조회
        List<ProductDto> items = switch (sortType) {
            case LATEST     -> productMapper.findByLatest(categoryId, catFrom, catTo, lastId, limit);
            case PRICE_DESC -> productMapper.findByPriceDesc(categoryId, catFrom, catTo, lastPrice, lastId, limit);
            case PRICE_ASC  -> productMapper.findByPriceAsc(categoryId,  catFrom, catTo, lastPrice, lastId, limit);
            case SALES      -> productMapper.findBySalesDesc(categoryId, catFrom, catTo, lastSales, lastId, limit);
        };

        // nextCursor 생성
        String nextCursor = null;
        boolean hasMore = items.size() == limit;
        if (!items.isEmpty()) {
            var last = items.get(items.size() - 1);
            Map<String,Object> next = switch (sortType) {
                case LATEST -> Map.of("id", last.getId());
                case PRICE_DESC, PRICE_ASC -> Map.of("price", last.getDiscountedPrice(), "id", last.getId());
                case SALES -> Map.of("sales", last.getSales(), "id", last.getId());
            };
            nextCursor = CursorUtil.encode(next);
        }

        return new ProductListResponse(items, totalCount, hasMore, nextCursor);
    }

    @Override
    public ProductDetailResponse getProductDetail(Long id) {
        // 1. 상품 상세 조회
        ProductDetailDto product = productMapper.selectProductDetail(id);
        if (product == null) {
            throw new RefitException(ErrorCode.ILLEGAL_ARGUMENT, "상품이 존재하지 않습니다. id=" + id);
        }

        // 2. 상품 이미지 리스트 조회
        List<ImageDto> images = productMapper.selectProductImages(id);

        return new ProductDetailResponse(product, images);
    }

    @Override
    public ProductListResponse searchProductsByName(String q, String bhType, SortType sort, int limit, String cursor) {
        var c = CursorUtil.decode(cursor);
        Long lastId       = CursorUtil.asLong(c.get("id"));
        Integer lastPrice = CursorUtil.asInt(c.get("price"));
        Integer lastSales = CursorUtil.asInt(c.get("sales"));

        Integer bhTypeNumber = null;
        if ("beauty".equalsIgnoreCase(bhType)) {
            bhTypeNumber = 0;
        } else if ("health".equalsIgnoreCase(bhType)) {
            bhTypeNumber = 1;
        }

        int totalCount = productMapper.countProductsByName(q, bhTypeNumber);

        List<ProductDto> items = switch (sort) {
            case LATEST     -> productMapper.searchByNameLatest(q, bhTypeNumber, lastId, limit);
            case PRICE_DESC -> productMapper.searchByNamePriceDesc(q, bhTypeNumber, lastPrice, lastId, limit);
            case PRICE_ASC  -> productMapper.searchByNamePriceAsc(q,  bhTypeNumber, lastPrice, lastId, limit);
            case SALES -> productMapper.searchByNameSalesDesc(q, bhTypeNumber, lastSales, lastId, limit);
        };

        String nextCursor = null;
        boolean hasMore = items.size() == limit;
        if (hasMore) {
            var last = items.get(items.size() - 1);
            Map<String,Object> next = switch (sort) {
                case LATEST     -> Map.of("id", last.getId());
                case PRICE_DESC, PRICE_ASC -> Map.of("price", last.getDiscountedPrice(), "id", last.getId());
                case SALES      -> Map.of("sales", last.getSales(), "id", last.getId());
            };
            nextCursor = CursorUtil.encode(next);
        }

        return new ProductListResponse(items, totalCount, hasMore, nextCursor);
    }

    @Override
    public ProductSuggestResponse suggestProducts(String keyword, int limit, String cursor) {
        Map<String, Object> c = CursorUtil.decode(cursor);
        Long lastId = CursorUtil.asLong(c.get("id"));

        List<ProductSimpleDto> items = productMapper.findSuggestProducts(keyword, limit, lastId);

        boolean hasMore = items.size() == limit;
        String nextCursor = null;
        if (hasMore) {
            ProductSimpleDto last = items.get(items.size() - 1);
            nextCursor = CursorUtil.encode(Map.of("id", last.getId()));
        }
        return new ProductSuggestResponse(items, hasMore, nextCursor);
    }

    @Override
    public ProductListResponse getLikedProducts(List<Long> likedItems) {
        List<ProductDto> items = productMapper.getLikedProducts(likedItems);
        int total = items.size();
        return new ProductListResponse(items, total, false, null);
    }

    @Override
    public ProductRecommendationResponse getRecommendations(
            int productType, Long memberId, String concernCode, String location, int topk, int limit
    ) {
        int pt  = normalizeProductType(productType);
        int lim = (limit <= 0) ? 10 : Math.min(limit, 200);

        String key = RecommendationCacheKey.build(pt, memberId, concernCode, location, topk, lim);

        // 캐시 조회 (Cache Hit → 즉시 반환)
        ProductRecommendationResponse cached = cacheRepo.get(key);
        if (cached != null && !CollectionUtils.isEmpty(cached.getItems())) {
            if (Math.random() < 0.3) {
                Collections.shuffle(cached.getItems());
            }
            return cached;
        }

        // 캐시 미스 → 외부 AI 호출
        // 외부 추천은 개인화가 필요하므로 유효한 memberId 필수
        if (memberId == null || memberId <= 0) {
            throw new RefitException(ErrorCode.INVALID_TOKEN, "외부 추천 조회에는 유효한 회원 ID가 필요합니다.");
        }

        // 외부 AI 추천 요청
        AiRecommendRequest req = AiRecommendRequest.builder()
                .memberId(memberId)
                .productType(pt)
                .preferCategoryId(null)
                .location(location)
                .topk(topk)
                .finalCount(lim)
                .build();

        AiRecommendResponse ai = aiClient.request(req);

        // 외부 응답 → 내부 DTO 변환 (할인율/할인가 계산 포함)
        List<ProductRecommendationItemDto> items = ai.getResults().stream()
                .limit(lim)
                .map(r -> {
                    long rate  = (r.getDiscountRate() == null ? 0L : r.getDiscountRate());
                    long price = (r.getPrice() == null ? 0L : r.getPrice().longValue());
                    long discounted = ((price * (100 - rate)) / 100L / 100L) * 100L;
                    return ProductRecommendationItemDto.builder()
                            .productId(r.getProductId() == null ? null : r.getProductId().longValue())
                            .thumbnailUrl(r.getThumbnailUrl())
                            .brandName(r.getBrand())
                            .productName(r.getName())
                            .discountRate(r.getDiscountRate())
                            .price(price)
                            .discountedPrice(discounted)
                            .build();
                })
                .collect(Collectors.toList());

        ProductRecommendationResponse finalResp = ProductRecommendationResponse.builder()
                .items(items)
                .build();

        // 캐싱 후 반환
        cacheRepo.put(key, finalResp, cacheTtlSec);

        return finalResp;
    }

    private int normalizeProductType(int productType) {
        // 0=전체, 1=뷰티, 2=헤어, 3=건강기능식품 (default: 전체)
        if (productType < 0 || productType > 3) return 0;
        return productType;
    }

    @Override
    public List<ProductDto> findTopProductsByOrderCount(int limit) {
        return productMapper.selectTopProductsByOrderCount(limit);
    }
}
