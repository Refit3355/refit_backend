package com.refit.app.domain.notification.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class RegisterDeviceRequest {
    private String platform;   // "android"
    private String fcmToken;
    private String deviceId;   // optional
}
