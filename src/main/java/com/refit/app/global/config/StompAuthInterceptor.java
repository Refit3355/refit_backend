package com.refit.app.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StompAuthInterceptor implements ChannelInterceptor {

    private final JwtProvider jwtProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        var acc = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (acc == null) return message;

        // 1) CONNECT 시 토큰 필수
        if (StompCommand.CONNECT.equals(acc.getCommand())) {
            String auth = acc.getFirstNativeHeader("Authorization"); // "Bearer xxx"
            if (auth == null || !auth.startsWith("Bearer ")) {
                throw new org.springframework.security.access.AccessDeniedException("JWT required");
            }
            var jws = jwtProvider.parseAndValidate(auth.substring(7)); // 유효성/만료 검사
            Long userId = jwtProvider.getMemberId(jws);

            // Principal 심기 (@AuthenticationPrincipal 대신 사용)
            var authentication =
                    new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                            userId, null, java.util.List.of());
            acc.setUser(authentication);
        }

        // 2) SUBSCRIBE/SEND 시 로그인 여부만 확인
        if (StompCommand.SUBSCRIBE.equals(acc.getCommand()) ||
                StompCommand.SEND.equals(acc.getCommand())) {
            boolean ok = acc.getUser() instanceof org.springframework.security.authentication.UsernamePasswordAuthenticationToken
                    || acc.getSessionAttributes().get("memberId") != null;
            if (!ok) throw new org.springframework.security.access.AccessDeniedException("login required");
        }

        return message;
    }
}
