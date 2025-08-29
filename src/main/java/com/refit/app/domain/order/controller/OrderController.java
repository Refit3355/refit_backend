package com.refit.app.domain.order.controller;

import com.refit.app.domain.memberProduct.model.ProductType;
import com.refit.app.domain.order.dto.OrderItemDto;
import com.refit.app.domain.order.dto.request.DraftOrderRequest;
import com.refit.app.domain.order.dto.response.DraftOrderResponse;
import com.refit.app.domain.order.dto.response.OrderItemListResponse;
import com.refit.app.domain.order.dto.response.UpdateOrderStatusResponse;
import com.refit.app.domain.order.service.OrderService;
import com.refit.app.global.util.SecurityUtil;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RequiredArgsConstructor
@RestController
@Slf4j
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    @GetMapping("unregistered-products")
    public ResponseEntity<OrderItemListResponse> getUnregisteredOrderItems(
            @RequestParam("type") ProductType type) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        List<OrderItemDto> orderItems = orderService.getUnregisteredOrderItems(memberId, type);
        return ResponseEntity.ok(new OrderItemListResponse(orderItems));
    }

    @PostMapping("/draft")
    public DraftOrderResponse createDraft(@RequestBody @Valid DraftOrderRequest req) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        return orderService.createDraft(memberId, req);
    }

    // 교환 신청
    @PostMapping("/{orderItemId}/exchange")
    public ResponseEntity<UpdateOrderStatusResponse> requestExchange(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long orderItemId
    ) {
        UpdateOrderStatusResponse res = orderService.updateOrderItemStatus(memberId, orderItemId, 4);
        return ResponseEntity.ok(res);
    }

    // 반품 신청
    @PostMapping("/{orderItemId}/return")
    public ResponseEntity<UpdateOrderStatusResponse> requestReturn(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long orderItemId
    ) {
        UpdateOrderStatusResponse res = orderService.updateOrderItemStatus(memberId, orderItemId, 6);
        return ResponseEntity.ok(res);
    }
}
