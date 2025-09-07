package com.refit.app.domain.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AutoConfirmTarget {
    private Long memberId;
    private Long orderItemId;
    private String productName;
    private String brandName;
}
