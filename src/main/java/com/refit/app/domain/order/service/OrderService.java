package com.refit.app.domain.order.service;

import com.refit.app.domain.memberProduct.model.ProductType;
import com.refit.app.domain.order.dto.AutoConfirmTarget;
import com.refit.app.domain.order.dto.OrderItemDto;
import com.refit.app.domain.order.dto.request.DraftOrderRequest;
import com.refit.app.domain.order.dto.response.DraftOrderResponse;
import com.refit.app.domain.order.dto.response.UpdateOrderStatusResponse;
import java.util.List;

public interface OrderService {

    List<OrderItemDto> getUnregisteredOrderItems(Long memberId, ProductType type);

    DraftOrderResponse createDraft(Long memberId, DraftOrderRequest req);

    UpdateOrderStatusResponse updateOrderItemStatus(Long memberId, Long orderItemId, int status);

    UpdateOrderStatusResponse confirmReceipt(Long memberId, Long orderItemId);

    int autoConfirmDeliveredOver5Days();

    List<AutoConfirmTarget> collectTargetsForAutoConfirm();
}
