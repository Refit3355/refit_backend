package com.refit.app.domain.me.service;

import com.refit.app.domain.me.dto.CombinationItemDto;
import com.refit.app.domain.me.dto.MyOrderDto;
import com.refit.app.domain.me.dto.MyOrderItemDto;
import com.refit.app.domain.me.dto.MyCombinationDto;
import com.refit.app.domain.me.dto.response.CombinationsResponse;
import com.refit.app.domain.me.dto.response.ProfileImageSaveResponse;
import com.refit.app.domain.me.dto.response.RecentMyOrderResponse;
import com.refit.app.domain.me.mapper.MeMapper;
import com.refit.app.infra.file.s3.S3Uploader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class MeServiceImpl implements MeService{

    private final MeMapper meMapper;
    private final S3Uploader s3Uploader;

    @Override
    @Transactional(readOnly = true)
    public RecentMyOrderResponse getRecentOrders(Long memberId) {
        // 1) 주문 + 주문 아이템 조회
        List<MyOrderItemDto> rows = meMapper.findOrdersByMemberId(memberId);

        // 2) orderCode 기준 그룹핑
        Map<String, List<MyOrderItemDto>> orderMap = rows.stream()
                .collect(Collectors.groupingBy(MyOrderItemDto::getOrderCode));

        // 3) 주문 DTO 조립
        List<MyOrderDto> orders = orderMap.entrySet().stream()
                .map(e -> {
                    List<MyOrderItemDto> items = e.getValue();
                    MyOrderItemDto first = items.get(0);

                    return MyOrderDto.builder()
                            .orderId(e.getKey())
                            .items(items)
                            .originalMerchandiseTotal(first.getOriginalMerchandiseTotal())
                            .currentMerchandiseSubtotal(first.getCurrentMerchandiseSubtotal())
                            .freeShippingApplied(first.getFreeShippingApplied())
                            .deliveryFee(first.getDeliveryFee())
                            .build();
                })
                .collect(Collectors.toList());

        return RecentMyOrderResponse.builder()
                .recentOrder(orders)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public CombinationsResponse getMyCombinations(Long memberId) {
        List<MyCombinationDto> combinations = meMapper.findCombinationsByMember(memberId);

        List<MyCombinationDto> dtoList = combinations.stream().map(combination -> {
            List<CombinationItemDto> items = meMapper.findCombinationItems(combination.getCombinationId());

            long originalTotalPrice = items.stream()
                    .mapToLong(CombinationItemDto::getPrice)
                    .sum();

            long discountedTotalPrice = items.stream()
                    .mapToLong(item -> {
                        long discounted = item.getPrice() - (item.getPrice() * item.getDiscountRate() / 100);
                        return Math.floorDiv(discounted, 100) * 100;
                    })
                    .sum();

            return MyCombinationDto.builder()
                    .combinationId(combination.getCombinationId())
                    .memberId(combination.getMemberId())
                    .nickname(combination.getNickname())
                    .profileUrl(combination.getProfileUrl())
                    .combinationName(combination.getCombinationName())
                    .likes(combination.getLikes())
                    .originalTotalPrice(originalTotalPrice)
                    .discountedTotalPrice(discountedTotalPrice)
                    .productImages(items.stream()
                            .map(CombinationItemDto::getThumbnailUrl)
                            .collect(Collectors.toList()))
                    .build();
        }).collect(Collectors.toList());

        return new CombinationsResponse(dtoList);
    }

    @Override
    @Transactional
    public ProfileImageSaveResponse updateProfileImage(Long memberId, MultipartFile profileImage) {
        // S3에 프사 파일 업로드
        String url = s3Uploader.uploadProfile(profileImage);

        // DB 업데이트
        meMapper.updateProfileImage(memberId, url);

        return new ProfileImageSaveResponse(url, "프로필 이미지가 성공적으로 업데이트되었습니다.");
    }
}
