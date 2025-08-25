package com.refit.app.domain.auth.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HealthInfoDto {

    @Min(0)
    @Max(1)
    private Integer eyeHealth;
    @Min(0)
    @Max(1)
    private Integer fatigue;
    @Min(0)
    @Max(1)
    private Integer sleepStress;
    @Min(0)
    @Max(1)
    private Integer immuneCare;
    @Min(0)
    @Max(1)
    private Integer muscleHealth;
    @Min(0)
    @Max(1)
    private Integer gutHealth;
    @Min(0)
    @Max(1)
    private Integer bloodCirculation;
}
