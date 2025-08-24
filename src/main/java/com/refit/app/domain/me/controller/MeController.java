package com.refit.app.domain.me.controller;

import com.refit.app.domain.me.dto.response.CombinationsResponse;
import com.refit.app.domain.me.dto.response.ProfileImageSaveResponse;
import com.refit.app.domain.me.dto.response.RecentMyOrderResponse;
import com.refit.app.domain.me.service.MeService;
import com.refit.app.infra.file.s3.S3Uploader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RequiredArgsConstructor
@RestController
@Slf4j
@RequestMapping("/me")
public class MeController {

    private final MeService meService;
    private final S3Uploader s3Uploader;

    /**
     * GET /me/orders
     */
    @GetMapping("/orders")
    public ResponseEntity<RecentMyOrderResponse> getRecentOrders(
            @AuthenticationPrincipal Long memberId
    ) {
        RecentMyOrderResponse resp = meService.getRecentOrders(memberId);
        return ResponseEntity.ok(resp);
    }

    /**
     * GET /me/combinations
     */
    @GetMapping("/combinations")
    public ResponseEntity<CombinationsResponse> getMyCombinations(
            @AuthenticationPrincipal Long memberId) {
        CombinationsResponse myCombinations = meService.getMyCombinations(memberId);
        return ResponseEntity.ok(myCombinations);
    }

    /**
     * POST /me/profile/image/update
     */
    @PostMapping("/profile/image/update")
    public ResponseEntity<ProfileImageSaveResponse> updateProfileImage(
            @AuthenticationPrincipal Long memberId,
            @RequestParam("profileImage") MultipartFile profileImage
    ) {
        ProfileImageSaveResponse response = meService.updateProfileImage(memberId, profileImage);
        return ResponseEntity.ok(response);
    }
}
