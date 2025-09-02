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

    // 화장품용 (그대로)
    private String riskyText;
    private String cautionText;
    private String safeText;

    // 공통 요약(백워드 호환)
    private String summary;

    // ⬇️ 영양제 전용 두 문단
    private String supplementBenefits;           // 상단: 효능(3–4줄)
    private String supplementConditionCautions;  // 하단: 질병/약물 관련 주의(2–4줄)
}
