package com.refit.app.domain.order.controller;

import com.refit.app.domain.memberProduct.model.ProductType;
import com.refit.app.domain.order.dto.OrderItemDto;
import com.refit.app.domain.order.dto.response.OrderItemListResponse;
import com.refit.app.domain.order.service.OrderService;
import com.refit.app.global.util.SecurityUtil;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
}
