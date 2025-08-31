package com.refit.app.domain.notification.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.*;
import com.refit.app.domain.notification.mapper.DeviceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PushServiceImpl implements PushService {

    private final DeviceMapper deviceMapper;

    @PostConstruct
    public void init() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                ClassPathResource resource = new ClassPathResource("firebase/firebase-adminsdk.json");
                InputStream serviceAccount = resource.getInputStream();

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();

                FirebaseApp.initializeApp(options);
                log.info("FirebaseApp initialized from classpath:firebase/firebase-adminsdk.json");
            }
        } catch (Exception e) {
            log.error("Failed to initialize FirebaseApp", e);
        }
    }

    @Override
    public void sendToMember(Long memberId, Map<String, String> data) {
        if (FirebaseApp.getApps().isEmpty()) {
            log.warn("Firebase not initialized. Skip sending.");
            return;
        }

        List<String> tokens = deviceMapper.selectTokensByMemberId(memberId);
        if (tokens == null || tokens.isEmpty()) {
            log.warn("No tokens for member {}", memberId);
            return;
        }

        String title = data.getOrDefault("title", "Re:fit");
        String body  = data.getOrDefault("body",  "새 알림이 도착했어요");

        int success = 0, failure = 0;
        for (String token : tokens) {
            try {
                Message msg = Message.builder()
                        .setToken(token)
                        .putAllData(data) // deeplink, type 등
                        .setNotification(
                                com.google.firebase.messaging.Notification.builder()
                                        .setTitle(title)
                                        .setBody(body)
                                        .build()
                        )
                        .build();

                String id = FirebaseMessaging.getInstance().send(msg);
                success++;
                log.debug("FCM sent ok token={}, id={}", token, id);

            } catch (FirebaseMessagingException e) {
                failure++;
                log.warn("FCM send failed token={} errCode={} msg={}",
                        token, e.getMessagingErrorCode(), e.getMessage());

                // 토큰 만료/삭제된 경우 DB 정리
                if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED
                        || e.getMessagingErrorCode() == MessagingErrorCode.INVALID_ARGUMENT) {
                    try {
//                        deviceMapper.deleteByToken(token);
//                        log.info("Deleted invalid FCM token from DB: {}", token);
                    } catch (Exception ignore) { }
                }
            } catch (Exception e) {
                failure++;
                log.error("FCM send error token={}", token, e);
            }
        }

        log.info("FCM summary for member {} => success={}, failure={}, total={}",
                memberId, success, failure, tokens.size());
    }
}
