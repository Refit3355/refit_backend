package com.refit.app.domain.order.dto.response;

import com.refit.app.domain.order.dto.OrderItemDto;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OrderItemListResponse {
    private List<OrderItemDto> data;
}
