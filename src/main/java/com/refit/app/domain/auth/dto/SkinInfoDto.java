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
public class SkinInfoDto {

    @Min(0)
    @Max(1)
    private Integer atopic;
    @Min(0)
    @Max(1)
    private Integer acne;
    @Min(0)
    @Max(1)
    private Integer whitening;
    @Min(0)
    @Max(1)
    private Integer sebum;
    @Min(0)
    @Max(1)
    private Integer innerDryness;
    @Min(0)
    @Max(1)
    private Integer wrinkles;
    @Min(0)
    @Max(1)
    private Integer enlargedPores;
    @Min(0)
    @Max(1)
    private Integer redness;
    @Min(0)
    @Max(1)
    private Integer keratin;
}
