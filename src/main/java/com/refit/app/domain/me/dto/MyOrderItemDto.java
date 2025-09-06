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
    private String orderCode;
    private String productName;
    private String thumbnailUrl;
    private ZonedDateTime createdAt;

    private Long unitPrice;          // ORDER_ITEM.ITEM_PRICE
    private Long originalUnitPrice;  // ORDER_ITEM.ORG_UNIT_PRICE
    private Long discountRate;
    private Long lineAmount;         // ORDER_ITEM.LINE_AMOUNT

    private Long status;
    private Long quantity;
    private Long canceledCount;
    private Long quantityRemaining;  // ITEM_COUNT - CANCELED_COUNT (쿼리에서 계산)
    private String brand;

    private Long originalMerchandiseTotal;
    private Long currentMerchandiseSubtotal;
    private Integer freeShippingApplied;
    private Long deliveryFee;
}
