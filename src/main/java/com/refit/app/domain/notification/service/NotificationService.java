package com.refit.app.domain.notification.service;

import com.refit.app.domain.notification.dto.response.NotificationListResponse;

public interface NotificationService {
    Long createAndSave(Long memberId, String title, String body, String type, String imageUrl, String deeplink);
    int getUnreadCount(Long memberId);
    int markAllRead(Long memberId);
    NotificationListResponse getList(Long memberId, int offset, int size);
    void sendAndSave(Long memberId, String title, String body, String type, String imageUrl, String deeplink);

}
