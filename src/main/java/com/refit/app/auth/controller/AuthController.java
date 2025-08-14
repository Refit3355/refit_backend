package com.refit.app.auth.controller;

import com.refit.app.auth.dto.HealthRequest;
import com.refit.app.auth.dto.SignupRequest;
import com.refit.app.auth.service.SignupService;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SignupService signupService;

    @PostMapping("/join")
    public ResponseEntity<?> signup(@Valid @RequestBody SignupRequest signupRequest) {
        Long id = signupService.signupBasic(signupRequest);
        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "회원 기본정보 저장",
                "data", Map.of("memberId", id)
        ));
    }

    @PutMapping("/join/{memberId}/concerns/health")
    public ResponseEntity<?> health(@PathVariable Long memberId,
            @Valid @RequestBody HealthRequest healthRequest) {
        signupService.upsertHealth(memberId, healthRequest);
        return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "건강 정보 저장 완료되었습니다."));
    }
}
