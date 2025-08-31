package com.refit.app.domain.order.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderLineItem {
    @NotNull
    private Long productId;

    @Positive
    private int quantity;
}
