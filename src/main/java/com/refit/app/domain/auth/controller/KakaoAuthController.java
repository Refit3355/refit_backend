package com.refit.app.domain.auth.controller;

import com.refit.app.domain.auth.dto.request.KakaoSignupRequest;
import com.refit.app.domain.auth.dto.request.KakaoVerifyRequest;
import com.refit.app.domain.auth.dto.response.KakaoVerifyResponse;
import com.refit.app.domain.auth.dto.response.LoginResponse;
import com.refit.app.domain.auth.dto.response.UtilResponse;
import com.refit.app.domain.auth.service.KakaoOAuthService;
import com.refit.app.global.config.JwtProvider;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/oauth/kakao")
@RequiredArgsConstructor
public class KakaoAuthController {

    private final KakaoOAuthService kakaoOAuthService;
    private final JwtProvider jwtProvider;

    /**
     * 클라가 보낸 kakao user access_token 검증 - 연동 O: 즉시 로그인 → Authorization 헤더에 access, 바디에 refreshToken
     * 포함 - 연동 X: needSignup=true + 프리필 데이터(nickname 등) 반환
     */
    @PostMapping("/verify")
    public UtilResponse<KakaoVerifyResponse> verify(
            @Valid @RequestBody KakaoVerifyRequest req,
            HttpServletResponse resp
    ) {
        KakaoVerifyResponse data = kakaoOAuthService.verify(req.getAccessToken());

        if (!data.isNeedSignup()) {
            // 바로 로그인 케이스: access 헤더 발급
            String access = jwtProvider.createAccessToken(
                    data.getMemberId(), data.getUserEmail(), data.getUserNickname());
            resp.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + access);
            // refreshToken은 KakaoVerifyResponse 안에 이미 세팅됨
        }

        return new UtilResponse<>("SUCCESS", "카카오 검증 완료", data);
    }

    /**
     * 회원가입(필수 정보 수집 후) - 가입 저장 + OAUTH_ID 연결 → access 헤더, refresh 바디
     */
    @PostMapping("/signup")
    public UtilResponse<LoginResponse> signup(
            @Valid @RequestBody KakaoSignupRequest req,
            HttpServletResponse resp
    ) {
        LoginResponse data = kakaoOAuthService.signupWithKakao(
                req.getAccessToken(), req.getSignupAll());

        // 토큰 발급
        String access = jwtProvider.createAccessToken(
                data.getMemberId(), req.getSignupAll().getSignup().getEmail(), data.getNickname());
        String refresh = jwtProvider.createRefreshToken(data.getMemberId());

        // 헤더/바디 세팅
        resp.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + access);
        data.setRefreshToken(refresh);

        return new UtilResponse<>("SUCCESS", "카카오 회원가입 및 로그인 완료", data);
    }
}
