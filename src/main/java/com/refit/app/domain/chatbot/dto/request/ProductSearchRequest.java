package com.refit.app.domain.chatbot.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSearchRequest {
    private Integer bhType;       // 뷰티/헬스 구분
    private List<Long> effectIds; // 효능 ID 배열
    private String sort;          // latest, popular, lowPrice, highPrice
    private Long lastId;          // 커서 ID
    private Integer limit;        // 조회 개수
}
