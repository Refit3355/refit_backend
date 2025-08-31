package com.refit.app.domain.order.dto;

import java.time.LocalDateTime;
import lombok.*;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class OrderInsertRow {

    // selectKey로 채워짐
    private Long orderId;

    // 기본 정보
    private Long memberId;
    private String orderCode;
    private String orderSummary;

    // 금액
    private Long totalPrice;   // goodsAmount + deliveryFee - discount
    private Long goodsAmount;  // 주문상품 할인가 합계(스냅샷)
    private Long deliveryFee;  // 0 또는 3000 (정책 스냅샷)
    private Long discount;     // 서비스 정책상 0이면 0

    // 배송지 스냅샷
    private String deliveryAddress;
    private Long zipcode;
    private String detailAddress;
    private String roadAddress;

    // 상태
    private Integer orderStatus; // 0=REQUESTED, 1=APPROVED, ...

    private LocalDateTime createdAt;
    private Long createdBy;
    private LocalDateTime updatedAt;
    private Long updatedBy;
}
