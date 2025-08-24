package com.refit.app.domain.combination.service;


import com.refit.app.domain.combination.dto.response.CombinationResponse;
import com.refit.app.domain.me.dto.response.CombinationsResponse;
import java.util.List;

public interface CombinationService {
    CombinationResponse getCombinationDetail(Long combinationId);

    CombinationsResponse getLikedCombinations(List<Long> ids);
}
