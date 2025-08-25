package com.refit.app.domain.analysis.dto;

import lombok.Data;

@Data
public class AnalysisHealthInfoDto {

    private Long memberId;
    private Integer bloodPressure;          // BLOOD_PRESSURE
    private Integer bloodGlucose;           // BLOOD_GLUCOSE
    private Integer totalCaloriesBurned;    // TOTAL_CALORIES_BURNED
    private Integer sleepSession;
}
