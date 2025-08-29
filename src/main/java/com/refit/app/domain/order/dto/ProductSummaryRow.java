package com.refit.app.domain.order.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class ProductSummaryRow {
    private Long id;
    private String name;
    private String brandName;
    private String thumbnailUrl;
    private Long discountRate;
    private Long originalPrice;
    private Long discountedPrice;
}