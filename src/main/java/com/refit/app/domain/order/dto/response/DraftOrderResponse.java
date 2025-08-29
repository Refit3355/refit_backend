package com.refit.app.domain.order.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.util.List;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DraftOrderResponse {

    // 주문코드 (결제창 & confirm에 동일 값 사용)
    @NotBlank
    private String orderCode;

    private Long   orderId;   // = ORDERS.ORDER_ID (숫자 PK)

    // 결제창 표기용 주문명 (예: "로션 외 1건")
    @NotBlank
    private String orderSummary;

    // 총 결제금액(배송비 포함 계산된 최종 합)
    @Positive
    private long totalAmount;

    // 배송지 정보
    @NotNull @Valid
    private ShippingInfo shipping;

    // 주문 상품 목록
    @NotNull @Valid
    private List<OrderItemSummary> items;
}