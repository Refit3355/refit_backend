package com.refit.app.domain.chatbot.service;

import com.refit.app.domain.chatbot.dto.ProductCursorDto;
import com.refit.app.domain.chatbot.dto.ProductSearchRequestDto;
import com.refit.app.domain.chatbot.dto.ProductSearchResponseDto;
import com.refit.app.domain.chatbot.dto.request.ProductSearchRequest;
import com.refit.app.domain.chatbot.dto.response.ProductSearchListResponse;
import com.refit.app.domain.chatbot.mapper.ChatbotMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatbotServiceImpl implements ChatbotService {

    private final ChatbotMapper chatbotMapper;

    @Override
    public ProductSearchListResponse searchProducts(ProductSearchRequest request) {

        ProductCursorDto cursorInfo = null;

        // lastId가 있을 때만 커서 정보 조회
        if (request.getLastId() != null) {
            cursorInfo = chatbotMapper.findCursorInfo(request.getLastId());
            if (cursorInfo == null) throw new RuntimeException("유효하지 않은 lastId");
        }

        ProductSearchRequestDto req = ProductSearchRequestDto.builder()
                .bhType(request.getBhType())
                .effectIds(request.getEffectIds())
                .sort(request.getSort())
                .lastId(request.getLastId())
                .limit(request.getLimit())
                .cursorCreatedAt(cursorInfo != null ? cursorInfo.getCreatedAt() : null)
                .cursorSales(cursorInfo != null ? cursorInfo.getSales() : null)
                .cursorDiscountedPrice(cursorInfo != null ? cursorInfo.getDiscountedPrice() : null)
                .effectCount(request.getEffectIds() != null ? request.getEffectIds().size() : null)
                .build();

        // 상품 리스트 조회
        List<ProductSearchResponseDto> items = chatbotMapper.searchProducts(req);

        // 할인 가격 계산
        items.forEach(item -> {
            long discounted = (item.getOriginalPrice() * (100 - item.getDiscountRate())) / 100;
            discounted = (discounted / 100) * 100; // 100원 단위 절삭
            item.setDiscountedPrice(discounted);
        });

        Long totalCount = chatbotMapper.countProducts(req);

        return ProductSearchListResponse.builder()
                .items(items)
                .totalCount(totalCount)
                .build();
    }

}
