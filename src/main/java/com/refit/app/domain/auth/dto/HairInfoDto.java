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
public class HairInfoDto {

    @Min(0)
    @Max(1)
    private Integer hairLoss;
    @Min(0)
    @Max(1)
    private Integer damagedHair;
    @Min(0)
    @Max(1)
    private Integer scalpTrouble;
    @Min(0)
    @Max(1)
    private Integer dandruff;
}
