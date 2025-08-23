package com.refit.app.domain.combination.service;

import com.refit.app.domain.combination.dto.CombinationProductDto;
import com.refit.app.domain.combination.dto.response.CombinationResponse;
import com.refit.app.domain.combination.mapper.CombinationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CombinationServiceImpl implements CombinationService {

    private final CombinationMapper combinationMapper;

    @Override
    public CombinationResponse getCombinationDetail(Long combinationId) {
        // 조합 기본정보
        CombinationResponse combination = combinationMapper.findCombinationById(combinationId);

        // 조합에 속한 상품들
        List<CombinationProductDto> products = combinationMapper.findProductsByCombinationId(combinationId)
                .stream()
                .map(p -> CombinationProductDto.builder()
                        .productId(p.getProductId())
                        .productName(p.getProductName())
                        .brandName(p.getBrandName())
                        .price(p.getPrice())
                        .discountRate(p.getDiscountRate())
                        .discountedPrice(p.getPrice() - (p.getPrice() * p.getDiscountRate() / 100))
                        .thumbnailUrl(p.getThumbnailUrl())
                        .build())
                .collect(Collectors.toList());

        int totalPrice = products.stream()
                .mapToInt(CombinationProductDto::getDiscountedPrice)
                .sum();

        return CombinationResponse.builder()
                .combinationId(combination.getCombinationId())
                .combinationName(combination.getCombinationName())
                .combinationDescription(combination.getCombinationDescription())
                .products(products)
                .totalPrice(totalPrice)
                .build();
    }
}
