package com.refit.app.domain.cart.mapper;

import com.refit.app.domain.cart.dto.CartDto;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CartMapper {

    List<CartDto> getCartList(@Param("memberId") Long memberId);

    Integer getCartCount(@Param("memberId") Long memberId);

    void insertCart(@Param("memberId") Long memberId,
            @Param("productId") Long productId,
            @Param("quantity") int quantity);

    CartDto findCartByMemberIdAndProductId(
            @Param("memberId") Long memberId,
            @Param("productId") long productId);

    void updateCartCount(@Param("cartId") Long cartId,
            @Param("updatedCount") int updatedCount,
            @Param("memberId") Long memberId);
}
