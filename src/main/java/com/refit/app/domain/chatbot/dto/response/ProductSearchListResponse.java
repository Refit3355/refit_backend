package com.refit.app.domain.chatbot.dto.response;

import com.refit.app.domain.chatbot.dto.ProductSearchResponseDto;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSearchListResponse {
    private List<ProductSearchResponseDto> items;
    private Long totalCount;
}