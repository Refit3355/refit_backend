package com.refit.app.domain.me.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyCombinationDto {
    private Long combinationId;        // 조합 ID
    private Long memberId;             // 작성자 ID
    private String nickname;           // 작성자 닉네임
    private String profileUrl;         // 작성자 프로필 URL
    private String combinationName;    // 조합 이름
    private Long likes;             // 저장 수
    private Long originalTotalPrice;   // 원가 합계
    private Long discountedTotalPrice;     // 최종가
    private List<String> productImages; // 상품 이미지
}

