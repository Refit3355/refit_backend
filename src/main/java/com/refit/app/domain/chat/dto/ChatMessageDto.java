package com.refit.app.domain.chat.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessageDto {
    private Long chatId;
    private Long categoryId;
    private Long memberId;
    private String nickname;
    private Long productId;
    private String message;
    private String profileUrl;
    private LocalDateTime createdAt;
}
