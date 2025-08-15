package com.refit.app.auth.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class HealthRequest {

    @NotNull
    private Integer eyeHealth;
    @NotNull
    private Integer fatigue;
    @NotNull
    private Integer sleepStress;
    @NotNull
    private Integer immuneCare;
    @NotNull
    private Integer muscleHealth;
    @NotNull
    private Integer gutHealth;
    @NotNull
    private Integer bloodCirculation;
}
