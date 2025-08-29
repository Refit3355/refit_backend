package com.refit.app.domain.chat.controller;

import com.refit.app.domain.chat.dto.request.ChatSendRequest;
import com.refit.app.domain.chat.dto.response.ChatMessageResponse;
import com.refit.app.domain.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

@RequiredArgsConstructor
@Controller
@Slf4j
@MessageMapping("/chat")
public class ChatStompController {

    private final SimpMessagingTemplate template;
    private final ChatService chatService;

    @MessageMapping("/send")
    public void send(@Payload ChatSendRequest req,
            SimpMessageHeaderAccessor headers,
            java.security.Principal principal) {

        Long memberId = null;
        if (principal instanceof UsernamePasswordAuthenticationToken a
                && a.getPrincipal() instanceof Long u) {
            memberId = u;
        }
        if (memberId == null) {
            Object mid = headers.getSessionAttributes().get("memberId");
            if (mid != null) memberId = Long.valueOf(String.valueOf(mid));
        }
        if (memberId == null) throw new AccessDeniedException("login required");

        ChatMessageResponse saved = chatService.saveMessage(memberId, req);
        Long catId = (saved.getCategoryId() != null) ? saved.getCategoryId() : req.getCategoryId();
        template.convertAndSend("/topic/chat." + catId, saved);
    }
}
