package com.refit.app.domain.auth.controller;

import com.refit.app.domain.auth.dto.request.SignupAllRequest;
import com.refit.app.domain.auth.dto.response.SignupResponse;
import com.refit.app.domain.auth.dto.response.UtilResponse;
import com.refit.app.domain.auth.service.SignupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class MemberController {

    private final SignupService signupService;

    @PostMapping("/join")
    public UtilResponse<SignupResponse> signupAll(@Valid @RequestBody SignupAllRequest req) {
        Long id = signupService.signupAll(req);
        return new UtilResponse<>("SUCCESS", "회원가입이 완료되었습니다.", new SignupResponse(id));
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
