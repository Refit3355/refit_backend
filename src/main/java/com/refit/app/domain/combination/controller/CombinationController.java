package com.refit.app.domain.combination.controller;

import com.refit.app.domain.combination.dto.request.CombinationCreateRequest;
import com.refit.app.domain.combination.dto.request.LikedCombinationRequest;
import com.refit.app.domain.combination.dto.response.CombinationCreateResponse;
import com.refit.app.domain.combination.dto.response.CombinationDetailResponse;
import com.refit.app.domain.combination.dto.response.CombinationLikeResponse;
import com.refit.app.domain.combination.dto.response.CombinationListResponse;
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
    public CombinationDetailResponse getCombinationDetail(@PathVariable Long combinationId) {
        return combinationService.getCombinationDetail(combinationId);
    }

    // 저장한 조합 목록 조회
    @PostMapping("/like")
    public ResponseEntity<CombinationsResponse> getLikedCombinations(@RequestBody LikedCombinationRequest request) {
        CombinationsResponse likedCombinations = combinationService.getLikedCombinations(
                request.getIds());
        return ResponseEntity.ok(likedCombinations);
    }

    // 조합 좋아요
    @PostMapping("/{combinationId}/like")
    public ResponseEntity<CombinationLikeResponse> likeCombination(
            @PathVariable Long combinationId) {
        return ResponseEntity.ok(combinationService.likeCombination(combinationId));
    }

    // 조합 좋아요 해재
    @PostMapping("/{combinationId}/dislike")
    public ResponseEntity<CombinationLikeResponse> dislikeCombination(
            @PathVariable Long combinationId) {
        return ResponseEntity.ok(combinationService.dislikeCombination(combinationId));
    }

    // 조합 조회
    @GetMapping
    public CombinationListResponse getCombinations(
            @RequestParam String type,   // all, beauty, health
            @RequestParam String sort,   // popular, latest, lowPrice, highPrice
            @RequestParam(required = false) Long combinationId, // 페이징 커서
            @RequestParam(defaultValue = "10") Integer limit,   // 한 번에 가져올 개수
            @RequestParam(required = false) String keyword,     // 검색어
            @RequestParam(required = false) String searchMode   // combination or product
    ) {
        return combinationService.getCombinations(type, sort, combinationId, limit, keyword, searchMode);
    }

    // 조합 등록
    @PostMapping("/create")
    public ResponseEntity<CombinationCreateResponse> create(
            @AuthenticationPrincipal Long memberId,
            @RequestBody CombinationCreateRequest req
    ) {
        CombinationCreateResponse res = combinationService.createCombination(memberId, req);
        return ResponseEntity.ok(res);
    }

}
