package com.refit.app.domain.chat.dto.response;

import com.refit.app.domain.product.dto.ProductDto;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ChatMessageResponse {
    private Long chatId;
    private Long categoryId;
    private Long memberId;
    private String nickname;
    private Long productId;
    private String message;
    private String profileUrl;
    private LocalDateTime createdAt;
    private ProductDto product;
}