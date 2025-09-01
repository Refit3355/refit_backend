package com.refit.app.domain.analysis.dto;

import lombok.Data;

@Data
public class AnalysisHealthInfoDto {

    private Long memberId;
    private Integer bloodPressure;
    private Integer bloodGlucose;
    private Integer totalCaloriesBurned;
    private Integer sleepSession;
}
