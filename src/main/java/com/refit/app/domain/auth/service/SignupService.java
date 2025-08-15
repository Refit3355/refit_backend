package com.refit.app.domain.auth.service;

import com.refit.app.domain.auth.dto.request.SignupAllRequest;

public interface SignupService {

	Long signupAll(SignupAllRequest req);

	boolean isEmailAvailable(String email);

	boolean isNicknameAvailable(String nickname);
}
