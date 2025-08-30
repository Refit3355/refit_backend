package com.refit.app.domain.product.mapper;

import com.refit.app.domain.product.dto.ImageDto;
import com.refit.app.domain.product.dto.ProductDetailDto;
import com.refit.app.domain.product.dto.ProductDto;
import com.refit.app.domain.product.dto.ProductSimpleDto;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ProductMapper {

    // 상품 목록 조회
    List<ProductDto> findByLatest(
            @Param("categoryId") Integer categoryId,
            @Param("catFrom") Integer catFrom,
            @Param("catTo") Integer catTo,
            @Param("lastId") Long lastId,
            @Param("limit") int limit
    );

    List<ProductDto> findByPriceDesc(
            @Param("categoryId") Integer categoryId,
            @Param("catFrom") Integer catFrom,
            @Param("catTo") Integer catTo,
            @Param("lastPrice") Integer lastPrice,
            @Param("lastId") Long lastId,
            @Param("limit") int limit
    );

    List<ProductDto> findByPriceAsc(
            @Param("categoryId") Integer categoryId,
            @Param("catFrom") Integer catFrom,
            @Param("catTo") Integer catTo,
            @Param("lastPrice") Integer lastPrice,
            @Param("lastId") Long lastId,
            @Param("limit") int limit
    );

    List<ProductDto> findBySalesDesc(
            @Param("categoryId") Integer categoryId,
            @Param("catFrom") Integer catFrom,
            @Param("catTo") Integer catTo,
            @Param("lastSales") Integer lastSales,
            @Param("lastId") Long lastId,
            @Param("limit") int limit
    );

    int countProducts(@Param("categoryId") Integer categoryId,
            @Param("catFrom") Integer catFrom,
            @Param("catTo") Integer catTo);

    // 상품 상세 조회
    ProductDetailDto selectProductDetail(@Param("id") Long id);

    List<ImageDto> selectProductImages(@Param("id") Long id);

    // 상품 검색 목록 조회
    List<ProductDto> searchByNameLatest(
            @Param("keyword") String keyword,
            @Param("bhType") Integer bhType,
            @Param("lastId") Long lastId,
            @Param("limit") int limit);

    List<ProductDto> searchByNamePriceDesc(
            @Param("keyword") String keyword,
            @Param("bhType") Integer bhType,
            @Param("lastPrice") Integer lastPrice,
            @Param("lastId") Long lastId,
            @Param("limit") int limit);

    List<ProductDto> searchByNamePriceAsc(
            @Param("keyword") String keyword,
            @Param("bhType") Integer bhType,
            @Param("lastPrice") Integer lastPrice,
            @Param("lastId") Long lastId,
            @Param("limit") int limit);

    List<ProductDto> searchByNameSalesDesc(
            @Param("keyword") String keyword,
            @Param("bhType") Integer bhType,
            @Param("lastSales") Integer lastSales,
            @Param("lastId") Long lastId,
            @Param("limit") int limit);

    int countProductsByName(
            @Param("keyword") String keyword,
            @Param("bhType") Integer bhType
    );

    List<ProductSimpleDto> findSuggestProducts(
            @Param("keyword") String keyword,
            @Param("limit") int limit,
            @Param("lastId") Long lastId);

    List<ProductDto> getLikedProducts(@Param("ids") List<Long> ids);

    List<ProductDto> selectTopProductsByOrderCount(@Param("limit") int limit);

    ProductDto selectProductSnippet(@Param("id") Long id);
}
