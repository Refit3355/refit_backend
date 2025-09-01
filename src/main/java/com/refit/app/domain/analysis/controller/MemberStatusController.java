package com.refit.app.domain.analysis.controller;

import com.refit.app.domain.analysis.dto.response.MemberStatusResponse;
import com.refit.app.domain.analysis.service.AnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/analysis")
@RequiredArgsConstructor
public class MemberStatusController {

    private final AnalysisService analysisService;

    @GetMapping("/status")
    public MemberStatusResponse getMyStatus(
            Authentication authentication) {
        Long memberId = (Long) authentication.getPrincipal();
        return analysisService.getMemberStatus(memberId);
    }
}
