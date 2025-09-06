package com.refit.app.domain.me.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyOrderDto {
    private String orderId;
    private List<MyOrderItemDto> items;

    private Long originalMerchandiseTotal;   // ORDERS.GOODS_AMOUNT (주문 당시 상품 총액, 배송비 제외)
    private Long currentMerchandiseSubtotal; // Σ unitPrice * (quantity - canceledCount)  (현재 시점)
    private Integer freeShippingApplied;     // (0/1)
    private Long deliveryFee;                // ORDERS.DELIVERY_FEE
}
