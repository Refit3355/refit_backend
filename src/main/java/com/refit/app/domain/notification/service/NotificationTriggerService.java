package com.refit.app.domain.notification.service;

public interface NotificationTriggerService {

    void notifyPaymentCompleted(Long memberId, Long orderId, String body);

    void notifyPaymentCanceled(Long memberId, Long orderId, String body);

    void notifyExpiryImminent(Long memberId, Long productId, String body);

    void saveAndPush(Long memberId, String title, String body, String imageUrl, String deeplink, String type);

    void notifyOrderAutoConfirmed(Long memberId, Long orderItemId, String body);
}
