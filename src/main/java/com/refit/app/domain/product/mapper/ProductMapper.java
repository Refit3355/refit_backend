package com.refit.app.domain.product.mapper;

import com.refit.app.domain.product.dto.ProductDto;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ProductMapper {

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
}
