package com.refit.app.domain.memberProduct.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductSimpleRow {
    private Long productId;
    private String productName;
    private String brandName;
    private Integer recommendedPeriod;
    private Integer bhType; // PRODUCT.BH_TYPE
}
