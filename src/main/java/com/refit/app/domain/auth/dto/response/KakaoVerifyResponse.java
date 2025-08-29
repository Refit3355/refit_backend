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
@AllArgsConstructor
@NoArgsConstructor
public class KakaoVerifyResponse {

    private boolean needSignup;
    private String kakaoId;
    private String email;
    private String nickname;
    private String profileImageUrl;

    // 바로 로그인 케이스에만 사용
    private Long memberId;
    private String userEmail;
    private String userNickname;
    private String name;
    private String refreshToken;

    private HealthInfoDto health;
    private HairInfoDto hair;
    private SkinInfoDto skin;
}
