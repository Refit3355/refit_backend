package com.refit.app.domain.notification.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.refit.app.domain.notification.mapper.NotificationMapper;
import com.refit.app.domain.notification.model.NotificationType;
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
    private final DeviceService deviceService;

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
    public void notifyExpiryImminent(Long memberId, Long productId, String body) {
        // 마이핏 목록으로 이동
        String deeplink = "app://myfit";
        saveAndPush(memberId, "소비기한 임박", body, null, deeplink, NotificationType.EXPIRY_IMMINENT.name());
    }

    private void saveAndPush(
            Long memberId,
            String title,
            String body,
            String imageUrl,   // null 허용
            String deeplink,   // null 허용
            String type
    ) {
        // 1) DB 저장
        //  - mapper XML은 selectKey(BEFORE)로 SEQ_NOTIFICATION.NEXTVAL을 채움
        //  - 다중 파라미터(@Param)여도 keyProperty="notificationId"에 값이 주입됨
        Long notificationId = null;
        notificationMapper.insertNotification(
                notificationId,
                memberId,
                title,
                body,
                imageUrl,
                deeplink,
                type
        );

        // 2) 푸시 발송 (FCM)
        List<String> tokens = deviceService.findFcmTokensByMemberId(memberId);
        if (tokens == null || tokens.isEmpty()) return;

        for (String token : tokens) {
            try {
                Message.Builder builder = Message.builder()
                        .setToken(token)
                        // data에는 null 금지 → 빈 문자열로 치환
                        .putData("deeplink", deeplink == null ? "" : deeplink)
                        .putData("type", type);

                builder.setNotification(
                        com.google.firebase.messaging.Notification.builder()
                                .setTitle(title)
                                .setBody(body)
                                .setImage(imageUrl) // null 허용
                                .build()
                );

                FirebaseMessaging.getInstance().send(builder.build());
            } catch (Exception e) {
                log.warn("FCM send failed for token={}, memberId={}, type={}", token, memberId, type, e);
            }
        }
    }
}
