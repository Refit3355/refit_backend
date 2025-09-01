package com.refit.app.domain.analysis.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisResponseDto {

    private String memberName;
    private int matchRate;

    private List<String> risky;
    private List<String> caution;
    private List<String> safe;

    // 카테고리별 쉬운 문단
    private String riskyText;
    private String cautionText;
    private String safeText;

    // 최종 종합 요약(영양제/화장품 공통)
    private String summary;
}