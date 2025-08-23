package com.refit.app.domain.combination.controller;

import com.refit.app.domain.combination.dto.response.CombinationResponse;
import com.refit.app.domain.combination.service.CombinationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/combinations")
@RequiredArgsConstructor
public class CombinationController {

    private final CombinationService combinationService;

    @GetMapping("/{combinationId}")
    public CombinationResponse getCombinationDetail(@PathVariable Long combinationId) {
        return combinationService.getCombinationDetail(combinationId);
    }
}
