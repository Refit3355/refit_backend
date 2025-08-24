package com.refit.app.domain.chat.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class ChatSendRequest {
    private Long memberId;
    private Long productId;        // 선택
    @NotNull private String message;
}