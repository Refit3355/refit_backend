package com.refit.app.domain.combination.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CombinationResponseDto {
    private Long combinationId;
    private Long memberId;
    private String nickname;
    private String profileUrl;
    private String combinationName;
    private Long likes;
    private Long originalTotalPrice;
    private Long discountedTotalPrice;
    private List<String> productImages;
    private LocalDateTime createdAt;
}
