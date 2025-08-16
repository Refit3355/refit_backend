package com.refit.app.domain.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HealthInfoDto {

    private Integer eyeHealth;
    private Integer fatigue;
    private Integer sleepStress;
    private Integer immuneCare;
    private Integer muscleHealth;
    private Integer gutHealth;
    private Integer bloodCirculation;
}
