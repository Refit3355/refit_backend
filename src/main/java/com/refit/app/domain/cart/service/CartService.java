package com.refit.app.domain.cart.service;

import com.refit.app.domain.cart.dto.CartDto;
import java.util.List;

public interface CartService {

    List<CartDto> getCartList(Long memberId);

    Integer getCartCount(Long memberId);
}
