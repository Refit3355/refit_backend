package com.refit.app.domain.payment.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemRowDto {
    private Long orderItemId;
    private Long orderId;
    private Long productId;

    // 수량/가격 스냅샷
    private Integer itemCount;
    private Integer canceledCount;
    private Long itemPrice;          // ITEM_PRICE (할인가)
    private Long orgUnitPrice;       // ORG_UNIT_PRICE (원가)
    private Integer discountRate;

    // 상품 스냅샷
    private String productName;
    private String brandName;
    private String thumbnailUrl;
}
