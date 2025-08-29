package com.refit.app.domain.combination.service;

import com.refit.app.domain.combination.dto.CombinationProductDto;
import com.refit.app.domain.combination.dto.CombinationResponseDto;
import com.refit.app.domain.combination.dto.response.CombinationDetailResponse;
import com.refit.app.domain.combination.dto.response.CombinationLikeResponse;
import com.refit.app.domain.combination.dto.response.CombinationListResponse;
import com.refit.app.domain.combination.dto.response.MyCombinationResponse;
import com.refit.app.domain.combination.mapper.CombinationMapper;
import com.refit.app.domain.me.dto.MyCombinationDto;
import com.refit.app.domain.me.dto.response.CombinationsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CombinationServiceImpl implements CombinationService {

    private final CombinationMapper combinationMapper;

    @Override
    @Transactional(readOnly = true)
    public CombinationsResponse getLikedCombinations(List<Long> ids) {
        List<MyCombinationDto> combinations = combinationMapper.findCombinationsByIds(ids);

        List<MyCombinationDto> dtoList = combinations.stream().map(combination -> {
            List<CombinationProductDto> items = combinationMapper.findProductsByCombinationId(combination.getCombinationId());

            long originalTotalPrice = items.stream().mapToLong(CombinationProductDto::getPrice).sum();
            long discountedTotalPrice = items.stream()
                    .mapToLong(item -> item.getPrice() - (item.getPrice() * item.getDiscountRate() / 100))
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
                    .productImages(items.stream().map(CombinationProductDto::getThumbnailUrl).toList())
                    .build();
        }).toList();

        return new CombinationsResponse(dtoList);
    }

    @Override
    @Transactional
    public CombinationLikeResponse likeCombination(Long combinationId) {
        int updated = combinationMapper.increaseLike(combinationId);
        if (updated == 0) {
            throw new IllegalArgumentException("존재하지 않는 조합입니다.");
        }
        return new CombinationLikeResponse(combinationId, "좋아요 등록 완료");
    }

    @Override
    @Transactional
    public CombinationLikeResponse dislikeCombination(Long combinationId) {
        int updated = combinationMapper.decreaseLike(combinationId);
        if (updated == 0) {
            throw new IllegalArgumentException("존재하지 않는 조합입니다.");
        }
        return new CombinationLikeResponse(combinationId, "좋아요 해제 완료");
    }

    @Override
    @Transactional
    public CombinationListResponse getCombinations(String type, String sort, Long combinationId, Integer limit) {
        Integer bhType = null;
        if ("beauty".equalsIgnoreCase(type)) bhType = 0;
        else if ("health".equalsIgnoreCase(type)) bhType = 1;

        List<CombinationResponseDto> combos = combinationMapper.findCombinations(bhType, sort, combinationId, limit);
        Long totalCount = combinationMapper.countCombinations(bhType);

        combos.forEach(c -> {
            List<String> images = combinationMapper.findProductImagesByCombinationId(c.getCombinationId());
            c.setProductImages(images);
        });

        return new CombinationListResponse(combos, totalCount);
    }

    @Override
    @Transactional(readOnly = true)
    public CombinationDetailResponse getCombinationDetail(Long combinationId) {
        CombinationDetailResponse detail = combinationMapper.findCombinationDetail(combinationId);
        detail.setProducts(combinationMapper.findCombinationProducts(combinationId));
        return detail;
    }

}
