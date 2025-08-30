package com.refit.app.domain.notification.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@AllArgsConstructor
public class BadgeResponse {
    private int unreadCount;
}
