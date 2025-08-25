package com.refit.app.domain.analysis.dto;

import lombok.Data;

@Data
public class AnalysisSkinConcernDto {

    private Integer skinType;
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
