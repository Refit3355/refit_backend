package com.refit.app.domain.chat.controller;

import com.refit.app.domain.chat.dto.request.ChatSendRequest;
import com.refit.app.domain.chat.dto.response.ChatMessageResponse;
import com.refit.app.domain.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RestController;


@RequiredArgsConstructor
@RestController
@Slf4j
@MessageMapping("/chat")
public class ChatStompController {

    private final SimpMessagingTemplate template;
    private final ChatService chatService;

    @MessageMapping("/send")
    public void send(@AuthenticationPrincipal Long memberId,
            ChatSendRequest req,
            SimpMessageHeaderAccessor headers) {

        // 1) 들어온 프레임 로그
        log.info("==== Chat SEND 요청 수신 ====");
        log.info("memberId from @AuthenticationPrincipal = {}", memberId);
        log.info("ChatSendRequest = {}", req);
        log.info("Session Attributes = {}", headers.getSessionAttributes());

        // 2) 서비스 호출
        ChatMessageResponse saved = chatService.saveMessage(memberId, req);

        // 3) DB 저장 후 응답 로그
        log.info("==== ChatMessageResponse (브로드캐스트 직전) ====");
        log.info("saved = {}", saved);

        // 4) 브로드캐스트
        template.convertAndSend("/topic/chat." + saved.getCategoryId(), saved);
    }
}
