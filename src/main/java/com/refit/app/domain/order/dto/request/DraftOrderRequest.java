package com.refit.app.domain.order.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.refit.app.domain.order.model.OrderSource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DraftOrderRequest {
    @NotNull
    private OrderSource source;   // 주문 생성 출처 : DIRECT(상품상세 바로구매) / CART(장바구니) / COMBINATION(묶음 등 확장용)

    @Valid
    @Size(min = 1, message = "lines가 비어있습니다.")
    private List<OrderLineItem> lines; // 개별 상품 주문일 때 사용

    private Long combinationId; //  특정 기획세트/조합 주문용
}
