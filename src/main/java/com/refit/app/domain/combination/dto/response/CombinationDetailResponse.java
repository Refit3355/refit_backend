package com.refit.app.domain.combination.dto.response;

import com.refit.app.domain.combination.dto.CombinationProductDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CombinationDetailResponse {
    private Long combinationId;
    private String combinationName;
    private String combinationDescription;

    private Long memberId;
    private String nickname;
    private String profileUrl;

    private Long originalTotalPrice;
    private Long discountedTotalPrice;

    private List<CombinationProductDto> products;
}

