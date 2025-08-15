package com.refit.app.domain.auth.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SkinRequest {

    @NotNull
    private Integer atopic;
    @NotNull
    private Integer acne;
    @NotNull
    private Integer whitening;
    @NotNull
    private Integer sebum;
    @NotNull
    private Integer innerDryness;
    @NotNull
    private Integer wrinkles;
    @NotNull
    private Integer enlargedPores;
    @NotNull
    private Integer redness;
    @NotNull
    private Integer keratin;
}
