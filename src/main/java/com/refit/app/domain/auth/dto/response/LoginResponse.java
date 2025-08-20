package com.refit.app.domain.auth.dto.response;


import com.refit.app.domain.auth.dto.HealthInfoDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private Long memberId;
    private String nickname;
    private HealthInfoDto health;
    private String refreshToken;
}
