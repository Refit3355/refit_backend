package com.refit.app.domain.auth.service;

import com.refit.app.domain.auth.dto.HealthRequest;
import com.refit.app.domain.auth.dto.SignupRequest;

public interface SignupService {

    Long signupBasic(SignupRequest signupRequest);

    void upsertHealth(Long memberId, HealthRequest healthRequest);

    boolean isEmailAvailable(String email);

    boolean isNicknameAvailable(String nickname);
}
