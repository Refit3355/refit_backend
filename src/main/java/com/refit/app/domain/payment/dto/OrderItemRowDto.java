package com.refit.app.domain.payment.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemRowDto {
    private Long orderItemId;
    private Long itemPrice;
    private Integer itemCount;
    private Integer canceledCount;
    private Long productId;
}
