package com.refit.app.global.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtil {

    private SecurityUtil() {
        // 유틸 클래스는 인스턴스화 방지
    }

    // SecurityContext에서 현재 로그인한 사용자의 memberId(Long)을 반환한다.
    public static Long getCurrentMemberId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalStateException("Unauthenticated: no authentication found in context");
        }

        String principal = authentication.getPrincipal().toString(); // memberId가 문자열로 들어있음
        try {
            return Long.parseLong(principal);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Invalid memberId in principal: " + principal, e);
        }
    }
}