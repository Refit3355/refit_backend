package com.refit.app.domain.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRowDto {
    private Long notificationId;
    private String title;
    private String body;
    private String imageUrl;
    private String deeplink;
    private String type;
    private Integer isRead;  // 0/1
    private OffsetDateTime createdAt;
    private OffsetDateTime readAt;
}
