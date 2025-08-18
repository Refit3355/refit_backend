package com.refit.app.domain.auth.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KakaoSignupRequest {

    @NotBlank
    private String accessToken;
    @Valid
    @NotNull
    private SignupAllRequest signupAll;
}
