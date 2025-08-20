package com.refit.app.domain.cart.service;

import com.refit.app.domain.cart.dto.CartDto;
import com.refit.app.domain.cart.dto.request.CartAddRequest;
import com.refit.app.domain.cart.mapper.CartMapper;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartMapper cartMapper;

    @Override
    public List<CartDto> getCartList(Long memberId) {
        return cartMapper.getCartList(memberId);
    }

    @Override
    public Integer getCartCount(Long memberId) {
        return cartMapper.getCartCount(memberId);
    }

    @Override
    public void addCart(Long memberId, long productId, int quantity) {

        CartDto existingCart = cartMapper.findCartByMemberIdAndProductId(memberId, productId);

        if (existingCart != null) {
            // 이미 존재하면 수량 증가
            int updatedCount = existingCart.getCartCnt() + quantity;
            cartMapper.updateCartCount(existingCart.getCartId(), updatedCount, memberId);
        } else {
            // 없으면 신규 추가
            cartMapper.insertCart(memberId, productId, quantity);
        }
    }

    @Override
    public void addCartBulk(Long memberId, List<CartAddRequest> requests) {
        for (CartAddRequest req : requests) {
            addCart(memberId, req.getProductId(), req.getQuantity());
        }
    }

    @Override
    public void deleteCart(Long memberId, Long cartId) {
        cartMapper.deleteCart(memberId, cartId);
    }

    @Override
    public void deleteCartItemsBulk(Long memberId, List<Long> deletedItems) {
        if (deletedItems == null || deletedItems.isEmpty()) return;

        // null 제거 + 중복 제거
        List<Long> ids = deletedItems.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        cartMapper.deleteCartItemsBulk(memberId, ids);
    }
}
