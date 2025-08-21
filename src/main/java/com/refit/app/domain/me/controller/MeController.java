package com.refit.app.domain.me.controller;

import com.refit.app.domain.me.dto.response.RecentMyOrderResponse;
import com.refit.app.domain.me.service.MeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@Slf4j
@RequestMapping("/me")
public class MeController {

    private final MeService meService;

    /**
     * GET /me/orders/recent
     */
    @GetMapping("/orders/recent")
    public ResponseEntity<RecentMyOrderResponse> getRecentOrders(
            @AuthenticationPrincipal Long memberId
    ) {
        RecentMyOrderResponse resp = meService.getRecentOrders(memberId);
        return ResponseEntity.ok(resp);
    }
}
