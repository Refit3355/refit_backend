package com.refit.app.domain.auth.controller;

import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.refit.app.domain.auth.dto.HealthRequest;
import com.refit.app.domain.auth.dto.SignupRequest;
import com.refit.app.domain.auth.dto.UtilResponse;
import com.refit.app.domain.auth.service.SignupService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class MemberController {

    private final SignupService signupService;

    @PostMapping("/join")
    public UtilResponse<Map<String, Object>> signup(
            @Valid @RequestBody SignupRequest signupRequest) {
        Long id = signupService.signupBasic(signupRequest);
        return new UtilResponse<>(
                "SUCCESS",
                "회원 기본정보 저장에 성공했습니다.",
                Map.of("memberId", id)
        );
    }

    @PutMapping("/join/{memberId}/concerns/health")
    public UtilResponse<String> health(@PathVariable Long memberId,
            @Valid @RequestBody HealthRequest healthRequest) {
        signupService.upsertHealth(memberId, healthRequest);
        return new UtilResponse<>("SUCCESS", "건강 정보 저장이 완료되었습니다.", null);
    }

    @GetMapping("/check/email")
    public UtilResponse<Boolean> checkEmail(@RequestParam String email) {
        boolean available = signupService.isEmailAvailable(email);
        return new UtilResponse<>("SUCCESS", "이메일 중복검사를 완료했습니다.", available);
    }

    @GetMapping("/check/nickname")
    public UtilResponse<Boolean> checkNickname(@RequestParam String nickname) {
        boolean available = signupService.isNicknameAvailable(nickname);
        return new UtilResponse<>("SUCCESS", "닉네임 중복검사를 완료했습니다.", available);
    }
}
