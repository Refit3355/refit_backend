package com.refit.app.domain.analysis.service;

import com.refit.app.domain.analysis.dto.response.AnalysisResponseDto;
import com.refit.app.domain.analysis.dto.response.MemberStatusResponse;

public interface AnalysisService {

    MemberStatusResponse getMemberStatus(Long memberId);

    AnalysisResponseDto analyzeImage(
            Long memberId,
            byte[] imageBytes,
            String productType,
            @org.springframework.lang.Nullable String filename,
            @org.springframework.lang.Nullable String contentType
    );

}
