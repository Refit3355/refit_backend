package com.refit.app.domain.analysis.dto.response;

import com.refit.app.domain.analysis.dto.AnalysisStatus;
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

    private String supplementBenefits;
    private String supplementConditionCautions;

    private AnalysisStatus status; // OK가 아니면 프론트는 다이얼로그를 띄움
    private String reason;         // 내부 디버그/로그용
    private String suggestion;
}
