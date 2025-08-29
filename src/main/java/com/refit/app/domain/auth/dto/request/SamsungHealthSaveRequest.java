package com.refit.app.domain.auth.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SamsungHealthSaveRequest {
    private Long steps;
    private Double totalKcal;
    private Double bloodGlucoseMgdl;
    private Double systolicMmhg;
    private Double diastolicMmhg;
    private Double intakeKcal;
    private Long sleepMinutes;
}
