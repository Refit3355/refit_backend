package com.refit.app.domain.combination.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CombinationCreateRequest {
    private String name;
    private String content;
    private String type; // beauty / health
    private Long product1Id;
    private Long product2Id;
    private Long product3Id;
    private Long product4Id;
    private Long product5Id;
    private Long product6Id;
}
