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
    private String orderNumber;
    private String productName;
    private String thumbnailUrl;
    private ZonedDateTime createdAt;
    private Integer price;
    private Integer originalPrice;
    private Integer status;
    private Integer quantity;
    private String brand;
}
