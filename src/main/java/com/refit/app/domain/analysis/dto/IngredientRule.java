package com.refit.app.domain.analysis.dto;

import lombok.Data;

@Data
public class IngredientRule {

    private Long ingredientRuleId;
    private String ingredientName;
    private int ingredientCategory;  // 0 SAFE, 1 CAUTION, 2 DANGER
    private String description;
}