package com.refit.app.domain.auth.dto.response;

import com.refit.app.domain.auth.dto.HairInfoDto;
import com.refit.app.domain.auth.dto.SkinInfoDto;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class HealthResponse {

    private int eyeHealth;
    private int fatigue;
    private SkinInfoDto skinInfo;
    private HairInfoDto hairInfo;
}
