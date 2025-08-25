package com.refit.app.domain.analysis.controller;

import java.util.Map;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class AiTestController {

    private final ChatClient chat;

    public AiTestController(ChatClient.Builder builder) {
        this.chat = builder.build();
    }

    /**
     * 1) 간단한 텍스트 핑 테스트
     */
    @GetMapping("/ping")
    public Map<String, Object> ping(@RequestParam(defaultValue = "pong") String word) {
        String rsp = chat.prompt()
                .user("애국가 1절만 알려줘")
                .call()
                .content();
        return Map.of("raw", rsp);
    }

}
