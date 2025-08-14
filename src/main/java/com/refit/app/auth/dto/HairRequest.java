package com.refit.app.auth.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class HairRequest {

    @NotNull
    private Integer hairLoss;
    @NotNull
    private Integer damagedHair;
    @NotNull
    private Integer scalpTrouble;
    @NotNull
    private Integer dandruff;
}
