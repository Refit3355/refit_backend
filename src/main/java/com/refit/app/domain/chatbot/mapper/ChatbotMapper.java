package com.refit.app.domain.chatbot.mapper;

import com.refit.app.domain.chatbot.dto.ProductCursorDto;
import com.refit.app.domain.chatbot.dto.ProductSearchRequestDto;
import com.refit.app.domain.chatbot.dto.ProductSearchResponseDto;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ChatbotMapper {
    ProductCursorDto findCursorInfo(Long productId);
    List<ProductSearchResponseDto> searchProducts(ProductSearchRequestDto request);
    Long countProducts(ProductSearchRequestDto request);
}
