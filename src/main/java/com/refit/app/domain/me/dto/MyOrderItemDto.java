package com.refit.app.domain.me.dto;

import java.time.ZonedDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyOrderItemDto {
    private Long orderItemId;
    private Long productId;
    private String orderNumber;
    private String productName;
    private String thumbnailUrl;
    private ZonedDateTime createdAt;
    private Long price;
    private Long originalPrice;
    private Long status;
    private Long quantity;
    private String brand;
}
