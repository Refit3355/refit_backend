package com.refit.app.domain.me.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyCombinationResponse {
    private Long combinationId;        // 조합 ID
    private String combinationName;    // 조합 이름
    private Long originalTotalPrice;   // 원가 합계
    private Long finalTotalPrice;      // 최종가 (할인 적용)
    private List<String> productImages; // 상품 이미지
}
