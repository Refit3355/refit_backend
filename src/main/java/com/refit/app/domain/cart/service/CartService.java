package com.refit.app.domain.cart.service;

import com.refit.app.domain.cart.dto.CartDto;
import com.refit.app.domain.cart.dto.request.CartAddRequest;
import java.util.List;

public interface CartService {

    List<CartDto> getCartList(Long memberId);

    Integer getCartCount(Long memberId);

    void addCart(Long memberId, long productId, int quantity);

    void addCartBulk(Long memberId, List<CartAddRequest> requests);

    void deleteCart(Long memberId, Long cartId);
}
