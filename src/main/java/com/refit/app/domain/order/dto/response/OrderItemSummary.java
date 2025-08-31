package com.refit.app.domain.order.dto.response;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemSummary {

    @NotNull
    private Long productId;

    @NotBlank
    private String productName;

    private String brandName;
    private String thumbnailUrl;

    @Positive
    private Long originalPrice;
    private Long discountRate;
    private Long price;

    @Positive
    private int quantity;
}
