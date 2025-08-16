package com.refit.app.domain.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SkinInfoDto {

    private Integer atopic;
    private Integer acne;
    private Integer whitening;
    private Integer sebum;
    private Integer innerDryness;
    private Integer wrinkles;
    private Integer enlargedPores;
    private Integer redness;
    private Integer keratin;
}
