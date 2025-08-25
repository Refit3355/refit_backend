package com.refit.app.domain.chat.dto.response;

import com.refit.app.domain.chat.dto.ChatMessageDto;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ChatListResponse {
    private List<ChatMessageDto> items;
    private String nextCursor;
    private boolean hasNext;
}