package com.refit.app.domain.notification.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.refit.app.domain.notification.mapper.NotificationMapper;
import com.refit.app.domain.notification.model.NotificationType;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationTriggerServiceImpl implements NotificationTriggerService {

    private final NotificationMapper notificationMapper;
    private final PushService pushService;

    @Override
    @Transactional
    public void notifyPaymentCompleted(Long memberId, Long orderId, String body) {
        String deeplink = "app://orders";
        saveAndPush(memberId, "결제 완료", body, null, deeplink, NotificationType.PAYMENT_COMPLETED.name());
    }

    @Override
    @Transactional
    public void notifyPaymentCanceled(Long memberId, Long orderId, String body) {
        String deeplink = "app://orders";
        saveAndPush(memberId, "결제 취소", body, null, deeplink, NotificationType.PAYMENT_CANCELED.name());
    }

    @Override
    @Transactional
    public void notifyExpiryImminent(Long memberId, Long memberProductId, String body) {
        // 마이핏 목록으로 이동
        String deeplink = "app://myfit";
        saveAndPush(memberId, "소비기한 임박", body, null, deeplink, NotificationType.EXPIRY_IMMINENT.name());
    }

    public void saveAndPush(
            Long memberId,
            String title,
            String body,
            String imageUrl,   // null 허용
            String deeplink,   // null 허용
            String type
    ) {
        // 1) DB 저장
        Long notificationId = null;
        notificationMapper.insertNotification(
                notificationId, memberId, title, body, imageUrl, deeplink, type
        );

        // 2) 전송은 PushService에 위임
        pushService.sendToMember(memberId, Map.of(
                "type", type,
                "title", title,
                "body", body,
                "deeplink", deeplink == null ? "" : deeplink,
                "imageUrl", imageUrl == null ? "" : imageUrl
        ));
    }
}
