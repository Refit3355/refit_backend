package com.refit.app.auth.service;

import com.refit.app.auth.dto.HealthRequest;
import com.refit.app.auth.dto.SignupRequest;

public interface SignupService {

    Long signupBasic(SignupRequest signupRequest);

    void upsertHealth(Long memberId, HealthRequest healthRequest);
}
