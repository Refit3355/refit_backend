package com.refit.app.domain.notification.service;

import java.util.List;

public interface DeviceService {
    void register(Long memberId, String platform, String fcmToken, String deviceId);
    void deleteByToken(Long memberId, String fcmToken);

    List<String> findFcmTokensByMemberId(Long memberId);
}
