package com.refit.app.domain.me.service;

import com.refit.app.domain.me.dto.MyOrderDto;
import com.refit.app.domain.me.dto.MyOrderItemDto;
import com.refit.app.domain.me.dto.response.RecentMyOrderResponse;
import com.refit.app.domain.me.mapper.MeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MeServiceImpl implements MeService{

    private final MeMapper meMapper;

    @Override
    public RecentMyOrderResponse getRecentOrders(Long memberId) {
        // 주문 + 주문 아이템 조회
        List<MyOrderItemDto> rows = meMapper.findOrdersByMemberId(memberId);

        // OrderId 기준으로 그룹핑
        Map<String, List<MyOrderItemDto>> orderMap = rows.stream()
                .map(row -> MyOrderItemDto.builder()
                        .orderItemId(row.getOrderItemId())
                        .orderNumber(row.getOrderNumber())
                        .productName(row.getProductName())
                        .thumbnailUrl(row.getThumbnailUrl())
                        .createdAt(row.getCreatedAt())
                        .price(row.getPrice())
                        .originalPrice(row.getOriginalPrice())
                        .status(row.getStatus())
                        .quantity(row.getQuantity())
                        .brand(row.getBrand())
                        .build())
                .collect(Collectors.groupingBy(MyOrderItemDto::getOrderNumber));

        List<MyOrderDto> orders = orderMap.entrySet().stream()
                .map(e -> MyOrderDto.builder()
                        .orderId(e.getKey()) // orderId 대신 orderNumber 기준
                        .items(e.getValue())
                        .build())
                .collect(Collectors.toList());

        return RecentMyOrderResponse.builder()
                .recentOrder(orders)
                .build();
    }
}
