package com.refit.app.domain.combination.dto.response;

import com.refit.app.domain.combination.dto.CombinationResponseDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CombinationListResponse {
    private List<CombinationResponseDto> combinations;
    private Long totalCount;
}
