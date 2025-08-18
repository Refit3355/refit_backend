package com.refit.app.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KakaoVerifyRequest {

    @NotBlank
    private String accessToken;
}
