package com.refit.app.domain.notification.service;

public interface NotificationTriggerService {

    void notifyPaymentCompleted(Long memberId, Long orderId, String body);

    void notifyPaymentCanceled(Long memberId, Long orderId, String body);

    void notifyExpiryImminent(Long memberId, Long productId, String body);
}
