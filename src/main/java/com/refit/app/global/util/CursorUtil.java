package com.refit.app.global.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

public class CursorUtil {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static String encode(Map<String, Object> cursorMap) {
        try {
            String json = objectMapper.writeValueAsString(cursorMap);
            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(json.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalArgumentException("Cursor encode failed", e);
        }
    }

    public static Map<String, Object> decode(String cursor) {
        if (cursor == null || cursor.isBlank()) return Map.of();
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(cursor);
            String json = new String(bytes, StandardCharsets.UTF_8);
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid cursor", e);
        }
    }

    public static Long asLong(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.longValue();
        return Long.valueOf(v.toString());
    }

    public static Integer asInt(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.intValue();
        return Integer.valueOf(v.toString());
    }
}
