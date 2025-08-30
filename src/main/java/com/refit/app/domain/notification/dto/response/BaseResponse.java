package com.refit.app.domain.notification.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BaseResponse {
    private boolean success = true;
    private String message;

    public static BaseResponse ok(String message) {
        return new BaseResponse(true, message);
    }

    public static BaseResponse fail(String message) {
        return new BaseResponse(false, message);
    }
}