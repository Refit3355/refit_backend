package com.refit.app.domain.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ReissueResultDto {

    private final Long userId;
    private final String accessToken;
    private final String rotatedRefreshToken;
}
