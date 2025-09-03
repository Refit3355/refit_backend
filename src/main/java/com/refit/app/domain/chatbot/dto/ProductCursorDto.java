package com.refit.app.domain.chatbot.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductCursorDto {
    private Long productId;
    private Long sales;
    private Long discountedPrice;
    private LocalDateTime createdAt;
}
