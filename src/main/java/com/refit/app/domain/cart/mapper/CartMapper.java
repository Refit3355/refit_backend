package com.refit.app.domain.cart.mapper;

import com.refit.app.domain.cart.dto.CartDto;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CartMapper {

    List<CartDto> getCartList(@Param("memberId") Long memberId);

    Integer getCartCount(@Param("memberId") Long memberId);
}
