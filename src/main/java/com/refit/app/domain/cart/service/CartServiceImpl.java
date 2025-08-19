package com.refit.app.domain.cart.service;

import com.refit.app.domain.cart.dto.CartDto;
import com.refit.app.domain.cart.mapper.CartMapper;
import java.util.List;
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
}
