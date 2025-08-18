package com.refit.app.domain.auth.service;

import com.refit.app.domain.auth.dto.request.SignupAllRequest;
import com.refit.app.domain.auth.dto.response.KakaoVerifyResponse;
import com.refit.app.domain.auth.dto.response.LoginResponse;

public interface KakaoOAuthService {

    KakaoVerifyResponse verify(String kakaoAccessToken);

    LoginResponse signupWithKakao(String kakaoAccessToken, SignupAllRequest signupAll);
}
