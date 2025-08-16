package com.refit.app.domain.auth.controller;

import com.refit.app.domain.auth.dto.ReissueResultDto;
import com.refit.app.domain.auth.dto.request.LoginRequest;
import com.refit.app.domain.auth.dto.request.SignupAllRequest;
import com.refit.app.domain.auth.dto.request.UpdateBasicRequest;
import com.refit.app.domain.auth.dto.response.LoginResponse;
import com.refit.app.domain.auth.dto.response.SignupResponse;
import com.refit.app.domain.auth.dto.response.UtilResponse;
import com.refit.app.domain.auth.service.MemberService;
import com.refit.app.global.config.JwtProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final JwtProvider jwtProvider;

    @PostMapping("/join")
    public UtilResponse<SignupResponse> signupAll(@Valid @RequestBody SignupAllRequest req) {
        Long id = memberService.signupAll(req);
        return new UtilResponse<>("SUCCESS", "회원가입이 완료되었습니다.", new SignupResponse(id));
    }

    @GetMapping("/check/email")
    public UtilResponse<Boolean> checkEmail(@RequestParam String email) {
        boolean available = memberService.isEmailAvailable(email);
        return new UtilResponse<>("SUCCESS", "이메일 중복검사를 완료했습니다.", available);
    }

    @GetMapping("/check/nickname")
    public UtilResponse<Boolean> checkNickname(@RequestParam String nickname) {
        boolean available = memberService.isNicknameAvailable(nickname);
        return new UtilResponse<>("SUCCESS", "닉네임 중복검사를 완료했습니다.", available);
    }

    @Value("${jwt.refresh-exp-days}")
    private long refreshExpDays;

    @PostMapping("/login")
    public UtilResponse<LoginResponse> login(
            @Valid @RequestBody LoginRequest req,
            HttpServletResponse httpResp
    ) {
        LoginResponse data = memberService.login(req.getEmail(), req.getPassword());

        // access token 헤더에 추가
        String accessToken = jwtProvider.createAccessToken(
                data.getMemberId(), data.getEmail(), data.getNickname());
        httpResp.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);

        // refresh token HttpOnly 쿠키에 저장
        String refreshToken = jwtProvider.createRefreshToken(data.getMemberId());
        boolean isLocal = true;
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(!isLocal)
                .sameSite(isLocal ? "Lax" : "None") // 이 부분 추후 수정 예정
                .path("/")
                .maxAge(refreshExpDays * 24L * 60L * 60L)
                .build();
        httpResp.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return new UtilResponse<>("SUCCESS", "로그인에 성공했습니다.", data);
    }

    @GetMapping("/refresh")
    public UtilResponse<Void> refreshToken(HttpServletRequest request,
            HttpServletResponse response) {
        // 1) 쿠키에서 refreshToken 추출
        String refreshToken = null;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if ("refreshToken".equals(c.getName())) {
                    refreshToken = c.getValue();
                    break;
                }
            }
        }
        if (refreshToken == null) {
            throw new IllegalArgumentException("리프레시 토큰 쿠키가 없습니다.");
        }

        ReissueResultDto res = memberService.reissueAccessToken(refreshToken);
        response.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + res.getAccessToken());

        if (res.getRotatedRefreshToken() != null) {
            ResponseCookie cookie = ResponseCookie.from("refreshToken",
                            res.getRotatedRefreshToken())
                    .httpOnly(true)
                    .secure(true)
                    .sameSite("None")
                    .path("/")
                    .maxAge(refreshExpDays * 24L * 60L * 60L)
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        }

        // 바디엔 토큰 안 보냄
        return new UtilResponse<>("SUCCESS", "토큰을 재발급했습니다.", null);
    }

    @PutMapping("/basic")
    public UtilResponse<Void> updateMyBasic(
            @Valid @RequestBody UpdateBasicRequest req,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        memberService.updateMyBasicInfo(userId, req);
        return new UtilResponse<>("SUCCESS", "기본 정보 수정을 완료했습니다.", null);
    }

}
