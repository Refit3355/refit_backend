package com.refit.app.domain.combination.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class LikedCombinationRequest {
    private List<Long> ids;   // 저장된 조합 ID들
}
