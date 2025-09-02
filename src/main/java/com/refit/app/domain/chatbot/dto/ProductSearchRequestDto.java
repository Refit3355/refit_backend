package com.refit.app.domain.chatbot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSearchRequestDto {
    private Integer bhType;       // 뷰티/헬스 구분
    private List<Long> effectIds; // 효능 ID 배열
    private String sort;          // latest, popular, lowPrice, highPrice
    private Long lastId;          // 커서 ID
    private Integer limit;        // 조회 개수

    // 커서 비교용 필드 (lastId 상품의 값)
    private LocalDateTime cursorCreatedAt;
    private Long cursorSales;
    private Long cursorDiscountedPrice;
    private Integer effectCount;
}
