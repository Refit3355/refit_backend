package com.refit.app.domain.combination.service;


import com.refit.app.domain.combination.dto.response.CombinationResponse;

public interface CombinationService {
    CombinationResponse getCombinationDetail(Long combinationId);
}
