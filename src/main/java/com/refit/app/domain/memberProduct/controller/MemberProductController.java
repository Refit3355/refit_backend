package com.refit.app.domain.memberProduct.controller;

import com.refit.app.domain.memberProduct.dto.request.MemberProductCreateRequest;
import com.refit.app.domain.memberProduct.service.MemberProductService;
import com.refit.app.global.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
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
        return ResponseEntity.ok().build();
    }

    @PostMapping("/custom")
    public ResponseEntity<Void> createCustom(@RequestBody MemberProductCreateRequest req) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        memberProductService.createCustom(memberId, req);
        return ResponseEntity.ok().build();
    }

}
