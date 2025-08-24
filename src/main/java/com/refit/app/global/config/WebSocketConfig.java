package com.refit.app.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 1) 네이티브 WebSocket (Postman/안드로이드 테스트용)
        registry.addEndpoint("/ws-stomp")
                .setAllowedOriginPatterns("*");

        // 2) SockJS (브라우저 폴백이 필요하면 별도로 유지)
        registry.addEndpoint("/ws-stomp-sockjs")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 메세지 구독 요청 url -> 메세지 받을 때
        registry.enableSimpleBroker("/topic");

        // 메세지 발행 요청 url -> 메세지 보낼 때
        registry.setApplicationDestinationPrefixes("/app");
    }
}