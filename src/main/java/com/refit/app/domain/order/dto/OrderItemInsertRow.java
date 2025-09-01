package com.refit.app.domain.order.dto;

import java.time.LocalDateTime;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class OrderItemInsertRow {
    private Long orderItemId; // selectKey
    private Long orderId;

    private Long productId;
    private String productName;
    private String brandName;
    private String thumbnailUrl;

    private Long memberId;
    private Integer  orderStatus;   // 0=REQUESTED
    private Integer  itemCount;
    private Long orgUnitPrice;  // 정가 스냅샷
    private Long  discountRate;  // %
    private Long itemPrice;     // 할인 단가(스냅샷)
    private Long lineAmount;    // itemPrice * itemCount

    private LocalDateTime createdAt;
    private Long createdBy;
    private LocalDateTime updatedAt;
    private Long updatedBy;
}
