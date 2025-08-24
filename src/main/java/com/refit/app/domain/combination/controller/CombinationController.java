package com.refit.app.domain.combination.controller;

import com.refit.app.domain.combination.dto.request.LikedCombinationRequest;
import com.refit.app.domain.combination.dto.response.CombinationResponse;
import com.refit.app.domain.combination.service.CombinationService;
import com.refit.app.domain.me.dto.response.CombinationsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/combinations")
@RequiredArgsConstructor
public class CombinationController {

    private final CombinationService combinationService;

    // 단일 조합 상세 조회
    @GetMapping("/{combinationId}")
    public ResponseEntity<CombinationResponse> getCombinationDetail(@PathVariable Long combinationId) {
        CombinationResponse combinationDetail = combinationService.getCombinationDetail(
                combinationId);
        return ResponseEntity.ok(combinationDetail);
    }

    // 저장한 조합 목록 조회
    @PostMapping("/like")
    public ResponseEntity<CombinationsResponse> getLikedCombinations(@RequestBody LikedCombinationRequest request) {
        CombinationsResponse likedCombinations = combinationService.getLikedCombinations(
                request.getIds());
        return ResponseEntity.ok(likedCombinations);
    }

}
