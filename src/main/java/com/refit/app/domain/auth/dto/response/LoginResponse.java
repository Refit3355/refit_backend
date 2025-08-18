package com.refit.app.domain.auth.dto.response;


import com.refit.app.domain.auth.dto.ConcernSummaryDto;
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
    private String email;
    private String nickname;
    private String name;
    private ConcernSummaryDto concerns;
    private String refreshToken;
}
