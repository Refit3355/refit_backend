package com.refit.app.domain.cart.dto;

import com.refit.app.domain.product.dto.ProductDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class CartDto {
    private long cartId;
    private int cartCnt;
    private Long id;
    private String thumbnailUrl;
    private String brandName;
    private String productName;
    private Integer discountRate;
    private int price;
    private int discountedPrice;
}
