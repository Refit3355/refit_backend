package com.refit.app.domain.auth.service;

import com.refit.app.domain.auth.dto.ConcernSummaryDto;
import com.refit.app.domain.auth.dto.ReissueResultDto;
import com.refit.app.domain.auth.dto.request.SignupAllRequest;
import com.refit.app.domain.auth.dto.request.UpdateBasicRequest;
import com.refit.app.domain.auth.dto.response.LoginResponse;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public interface MemberService {

    Long signupAll(SignupAllRequest req);

    boolean isEmailAvailable(String email);

    boolean isNicknameAvailable(String nickname);

    LoginResponse login(@Email @NotBlank String email, @NotBlank String password);

    String issueRefreshTokenCookieValue(Long memberId);

    ReissueResultDto reissueAccessToken(String refreshToken);

    void updateMyBasicInfo(Long memberIdFromToken, UpdateBasicRequest req);

    void updateMyConcerns(Long memberId, ConcernSummaryDto req);
}
