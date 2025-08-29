package com.refit.app.domain.analysis.service;

import com.refit.app.domain.analysis.dto.response.IngredientAnalysisResponse;
import java.util.List;

public interface IngredientReportService {

    IngredientAnalysisResponse analyze(Long memberId, List<String> ingredients);
}