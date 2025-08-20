package com.refit.app.domain.cart.dto.response;

import com.refit.app.domain.cart.dto.CartDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.List;

@Getter
@AllArgsConstructor
public class CartListResponse {
    private List<CartDto> data;
}
