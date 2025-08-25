package com.refit.app.domain.analysis.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberStatusResponse {

    private Long memberId;
    private Integer skinType;
    private String skinTypeName;
    private List<String> skinConcerns;
    private List<String> healthConcerns;
    private Metrics metrics;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Metrics {

        private Integer bloodPressure;
        private Integer bloodGlucose;
        private Integer totalCaloriesBurned;
        private Integer sleepSession;
    }
}
