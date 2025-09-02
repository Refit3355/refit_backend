package com.refit.app.domain.chatbot.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductSearchResponseDto {
    private Long productId;
    private String thumbnailUrl;
    private String brandName;
    private String productName;
    private Long discountRate;
    private Long originalPrice;
    private Long discountedPrice;
    private Long sales;
}