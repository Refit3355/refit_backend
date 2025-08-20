package com.refit.app.domain.memberProduct.controller;

import com.refit.app.domain.memberProduct.dto.request.MemberProductCreateRequest;
import com.refit.app.domain.memberProduct.dto.request.MemberProductUpdateRequest;
import com.refit.app.domain.memberProduct.dto.response.MemberProductDetailResponse;
import com.refit.app.domain.memberProduct.dto.response.MemberProductListResponse;
import com.refit.app.domain.memberProduct.model.ProductType;
import com.refit.app.domain.memberProduct.model.UsageStatus;
import com.refit.app.domain.memberProduct.service.MemberProductService;
import com.refit.app.global.util.SecurityUtil;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@Slf4j
@RequestMapping("/member-products")
public class MemberProductController {

    private final MemberProductService memberProductService;

    @PostMapping
    public ResponseEntity<Void> createFromProduct(@RequestParam("productId") Long productId) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        memberProductService.createFromProduct(memberId, productId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/custom")
    public ResponseEntity<Void> createCustom(@RequestBody MemberProductCreateRequest req) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        memberProductService.createCustom(memberId, req);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/order-item/{orderItemId}")
    public ResponseEntity<Void> createFromOrderItem(@PathVariable("orderItemId") Long orderItemId) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        memberProductService.createFromOrderItem(memberId, orderItemId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<MemberProductListResponse> getMemberProducts(
            @RequestParam("type") ProductType type,
            @RequestParam(name="status", defaultValue = "all") UsageStatus status
    ) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        List<MemberProductDetailResponse> details = memberProductService.getMemberProducts(memberId, type, status);
        MemberProductListResponse res = MemberProductListResponse.builder()
                .items(details)
                .total(details.size())
                .build();
        return ResponseEntity.ok(res);
    }

    @DeleteMapping("/{memberProductId}")
    public ResponseEntity<Void> deleteMemberProduct(
            @PathVariable("memberProductId") Long memberProductId) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        memberProductService.deleteMemberProduct(memberId, memberProductId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{memberProductId}/status")
    public ResponseEntity<Void> updateStatus(
            @PathVariable("memberProductId") Long memberProductId,
            @RequestParam("status") UsageStatus status
    ) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        memberProductService.updateStatus(memberId, memberProductId, status);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{memberProductId}")
    public ResponseEntity<Void> updateMemberProduct(
            @PathVariable("memberProductId") Long memberProductId,
            @RequestBody MemberProductUpdateRequest request) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        memberProductService.updateMemberProduct(memberId, memberProductId, request);
        return ResponseEntity.noContent().build();
    }
}
