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
}
