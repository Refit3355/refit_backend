package com.refit.app.domain.chatbot.controller;

import com.refit.app.domain.chatbot.dto.request.ProductSearchRequest;
import com.refit.app.domain.chatbot.dto.response.ProductSearchListResponse;
import com.refit.app.domain.chatbot.service.ChatbotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;

    @PostMapping("/products")
    public ResponseEntity<ProductSearchListResponse> searchProducts(@RequestBody ProductSearchRequest request) {
        return ResponseEntity.ok(chatbotService.searchProducts(request));
    }
}

