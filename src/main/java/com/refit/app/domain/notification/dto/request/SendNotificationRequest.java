package com.refit.app.domain.notification.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class SendNotificationRequest {
    @NotNull
    private Long memberId;
    @NotBlank
    private String title;
    @NotBlank
    private String body;
    @NotBlank
    private String type;       // ex) ORDER, PROMO
    private String imageUrl;   //
    private String deeplink;   // ex) app://order/123
}
