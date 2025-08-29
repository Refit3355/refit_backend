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
public class IngredientAnalysisResponse {

    private Long memberId;
    private int totalIngredients;
    private double matchRate; // %
    private List<String> safeIngredients;
    private List<String> cautionIngredients;
    private List<String> riskyIngredients;
    private String summary;
}
