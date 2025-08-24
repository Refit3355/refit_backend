package com.refit.app.domain.chat.service;

import com.refit.app.domain.chat.dto.request.ChatSendRequest;
import com.refit.app.domain.chat.dto.response.ChatMessageResponse;
import com.refit.app.domain.chat.mapper.ChatMapper;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatMapper chatMapper;

    @Transactional
    @Override
    public ChatMessageResponse saveMessage(Long chattingRoomId, ChatSendRequest req) {

        // 1) INSERT: 요청 DTO 그대로 저장
        Map<String, Object> param = new HashMap<>();
        param.put("req", req);
        chatMapper.insertFromRequest(param);

        Long chatId = (Long) param.get("chatId");
        if (chatId == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "채팅 저장 실패(chatId null)");
        }

        // 2) SELECT JOIN: 방금 저장한 행을 member와 조인해서 닉네임 포함 응답으로
        ChatMessageResponse saved = chatMapper.findByIdWithNickname(chatId);
        if (saved == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "채팅 조회 실패");
        }
        return saved;
    }
}
