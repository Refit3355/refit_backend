package com.refit.app.domain.auth.dto.response;


import com.refit.app.domain.auth.dto.HairInfoDto;
import com.refit.app.domain.auth.dto.HealthInfoDto;
import com.refit.app.domain.auth.dto.SkinInfoDto;
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
    private HairInfoDto hair;
    private SkinInfoDto skin;
    private String profileImageUrl;
    private String refreshToken;
}
