package com.refit.app.domain.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSimpleDto {
    private Long id;
    private String thumbnailUrl;
    private String productName;
}
