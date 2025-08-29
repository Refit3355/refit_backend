package com.refit.app.domain.me.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CombinationItemDto {
    private Long productId;
    private String thumbnailUrl;
    private Long price;
    private Integer discountRate;
}
