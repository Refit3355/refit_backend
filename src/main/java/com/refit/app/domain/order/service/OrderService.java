package com.refit.app.domain.order.service;

import com.refit.app.domain.memberProduct.model.ProductType;
import com.refit.app.domain.order.dto.OrderItemDto;
import com.refit.app.domain.order.dto.response.UpdateOrderStatusResponse;
import java.util.List;

public interface OrderService {

    List<OrderItemDto> getUnregisteredOrderItems(Long memberId, ProductType type);

    UpdateOrderStatusResponse updateOrderItemStatus(Long memberId, Long orderItemId, int status);
}
