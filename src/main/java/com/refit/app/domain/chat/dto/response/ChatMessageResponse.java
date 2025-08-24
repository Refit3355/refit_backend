package com.refit.app.domain.chat.dto.response;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChatMessageResponse {
    private Long categoryId;
    private Long memberId;
    private String nickname;
    private Long productId;
    private String message;
    private LocalDateTime createdAt;
}