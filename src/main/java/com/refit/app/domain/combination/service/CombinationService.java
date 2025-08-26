package com.refit.app.domain.combination.service;


import com.refit.app.domain.combination.dto.response.CombinationLikeResponse;
import com.refit.app.domain.combination.dto.response.CombinationListResponse;
import com.refit.app.domain.combination.dto.response.MyCombinationResponse;
import com.refit.app.domain.me.dto.response.CombinationsResponse;
import java.util.List;

public interface CombinationService {
    MyCombinationResponse getCombinationDetail(Long combinationId);

    CombinationsResponse getLikedCombinations(List<Long> ids);

    CombinationLikeResponse likeCombination(Long combinationId);

    CombinationLikeResponse dislikeCombination(Long combinationId);

    CombinationListResponse getCombinations(String type, String sort, Long combinationId, Integer limit);
}
