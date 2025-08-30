package com.refit.app.domain.notification.service;

import java.util.Map;

public interface PushService {
    void sendToMember(Long memberId, Map<String, String> data);
}
