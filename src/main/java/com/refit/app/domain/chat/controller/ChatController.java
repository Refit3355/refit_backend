package com.refit.app.domain.chat.controller;

import com.refit.app.domain.chat.dto.response.ChatListResponse;
import com.refit.app.domain.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/chat")
public class ChatController {

    private final ChatService chatService;

    @GetMapping("/{categoryId}/messages")
    public ResponseEntity<ChatListResponse> getChatMessages(
            @PathVariable("categoryId") Long categoryId,
            @RequestParam(name = "cursor", required = false) String cursor,
            @RequestParam(name = "size", defaultValue = "10") Integer size
    ) {
        return ResponseEntity.ok(chatService.getHistory(categoryId, cursor, size));
    }
}
