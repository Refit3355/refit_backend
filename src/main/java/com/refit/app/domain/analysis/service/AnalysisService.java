package com.refit.app.domain.analysis.service;

import com.refit.app.domain.analysis.dto.response.MemberStatusResponse;

public interface AnalysisService {

    MemberStatusResponse getMemberStatus(Long memberId);

}
