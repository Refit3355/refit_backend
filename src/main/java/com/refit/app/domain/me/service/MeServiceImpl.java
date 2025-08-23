package com.refit.app.domain.me.service;

import com.refit.app.domain.me.dto.CombinationItemDto;
import com.refit.app.domain.me.dto.MyOrderDto;
import com.refit.app.domain.me.dto.MyOrderItemDto;
import com.refit.app.domain.me.dto.response.MyCombinationResponse;
import com.refit.app.domain.me.dto.response.RecentMyOrderResponse;
import com.refit.app.domain.me.mapper.MeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MeServiceImpl implements MeService{

    private final MeMapper meMapper;

    @Override
    @Transactional(readOnly = true)
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

    @Override
    @Transactional(readOnly = true)
    public List<MyCombinationResponse> getMyCombinations(Long memberId) {
        List<MyCombinationResponse> combinations = meMapper.findCombinationsByMember(memberId);

        return combinations.stream().map(combination -> {
            List<CombinationItemDto> items = meMapper.findCombinationItems(combination.getCombinationId());

            long originalTotalPrice = items.stream()
                    .mapToLong(CombinationItemDto::getPrice)
                    .sum();

            // 각 상품별 할인율 적용 후 합산
            long finalTotalPrice = items.stream()
                    .mapToLong(item -> item.getPrice() - (item.getPrice() * item.getDiscountRate() / 100))
                    .sum();

            return MyCombinationResponse.builder()
                    .combinationId(combination.getCombinationId())
                    .combinationName(combination.getCombinationName())
                    .originalTotalPrice(originalTotalPrice)
                    .finalTotalPrice(finalTotalPrice)
                    .productImages(items.stream()
                            .map(CombinationItemDto::getThumbnailUrl)
                            .collect(Collectors.toList()))
                    .build();
        }).collect(Collectors.toList());
    }
}
