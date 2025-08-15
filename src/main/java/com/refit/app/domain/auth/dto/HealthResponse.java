package com.refit.app.domain.auth.dto;

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
