package com.refit.app.domain.chat.service;

import com.refit.app.domain.chat.dto.request.ChatSendRequest;
import com.refit.app.domain.chat.dto.response.ChatListResponse;
import com.refit.app.domain.chat.dto.response.ChatMessageResponse;

public interface ChatService {
    ChatMessageResponse saveMessage(Long chattingRoomId, ChatSendRequest req);

    ChatListResponse getHistory(Long categoryId, String cursor, Integer size);
}
