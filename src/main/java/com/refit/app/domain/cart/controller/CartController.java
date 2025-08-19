package com.refit.app.domain.cart.controller;

import com.refit.app.domain.auth.dto.response.UtilResponse;
import com.refit.app.domain.cart.dto.CartDto;
import com.refit.app.domain.cart.dto.request.CartAddRequest;
import com.refit.app.domain.cart.dto.response.CartCountResponse;
import com.refit.app.domain.cart.dto.response.CartListResponse;
import com.refit.app.domain.cart.service.CartService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@Slf4j
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<CartListResponse> getCartList(@AuthenticationPrincipal Long memberId) {
        List<CartDto> cartList = cartService.getCartList(memberId);
        return ResponseEntity.ok(new CartListResponse(cartList));
    }

    @GetMapping("/count")
    public ResponseEntity<CartCountResponse> getCartCount(@AuthenticationPrincipal Long memberId) {
        Integer count = cartService.getCartCount(memberId);
        return ResponseEntity.ok(new CartCountResponse(count));
    }

    @PostMapping("/items")
    public UtilResponse addCart(
            @AuthenticationPrincipal Long memberId,
            @RequestBody CartAddRequest request
    ) {
        cartService.addCart(memberId, request.getProductId(), request.getQuantity());
        return new UtilResponse<>("SUCCESS", "장바구니 담기를 성공했습니다.", null);
    }
}
