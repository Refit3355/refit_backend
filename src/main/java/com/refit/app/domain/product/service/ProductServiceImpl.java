package com.refit.app.domain.product.service;

import com.refit.app.domain.product.dto.ImageDto;
import com.refit.app.domain.product.dto.ProductDetailDto;
import com.refit.app.domain.product.dto.ProductSimpleDto;
import com.refit.app.domain.product.dto.response.ProductDetailResponse;
import com.refit.app.domain.product.dto.response.ProductListResponse;
import com.refit.app.domain.product.dto.ProductDto;
import com.refit.app.domain.product.dto.response.ProductSuggestResponse;
import com.refit.app.domain.product.model.SortType;
import com.refit.app.domain.product.mapper.ProductMapper;
import com.refit.app.global.exception.ErrorCode;
import com.refit.app.global.exception.RefitException;
import com.refit.app.global.util.CursorUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductMapper productMapper;

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
    public ProductListResponse searchProductsByName(String q, SortType sort, int limit, String cursor) {
        var c = CursorUtil.decode(cursor);
        Long lastId       = CursorUtil.asLong(c.get("id"));
        Integer lastPrice = CursorUtil.asInt(c.get("price"));
        Integer lastSales = CursorUtil.asInt(c.get("sales"));

        int totalCount = productMapper.countProductsByName(q);

        List<ProductDto> items = switch (sort) {
            case LATEST     -> productMapper.searchByNameLatest(q, lastId, limit);
            case PRICE_DESC -> productMapper.searchByNamePriceDesc(q, lastPrice, lastId, limit);
            case PRICE_ASC  -> productMapper.searchByNamePriceAsc(q,  lastPrice, lastId, limit);
            case SALES -> productMapper.searchByNameSalesDesc(q, lastSales, lastId, limit);
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


}
