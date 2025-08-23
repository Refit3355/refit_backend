package com.refit.app.domain.combination.dto.response;

import com.refit.app.domain.combination.dto.CombinationProductDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CombinationResponse {
    private Long combinationId;
    private String combinationName;
    private String combinationDescription;
    private int totalPrice;
    private List<CombinationProductDto> products;
}

