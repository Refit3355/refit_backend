package com.refit.app.domain.notification.service;

import com.refit.app.domain.notification.dto.NotificationRowDto;
import com.refit.app.domain.notification.dto.response.NotificationListResponse;
import com.refit.app.domain.notification.mapper.NotificationMapper;
import com.refit.app.domain.notification.model.NotificationType;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;
    private final NotificationTriggerService notificationTriggerService;

    @Override
    @Transactional
    public Long createAndSave(Long memberId, String title, String body, String type, String imageUrl, String deeplink) {
        Long id = null; // selectKey에서 채워짐
        notificationMapper.insertNotification(id, memberId, title, body, imageUrl, deeplink, type);
        // insertNotification 호출 이후 id 값이 바인딩 되므로 다시 조회 필요 시 반환값 관리 별도 가능
        // 여기서는 selectKey 결과를 id 변수에 못받으니 다시 쿼리 없이 반환은 null이 됨
        return id;
    }

    @Override
    public int getUnreadCount(Long memberId) {
        return notificationMapper.selectUnreadCount(memberId);
    }

    @Override
    @Transactional
    public int markAllRead(Long memberId) {
        return notificationMapper.markAllRead(memberId);
    }

    @Override
    public NotificationListResponse getList(Long memberId, int offset, int size) {
        List<NotificationRowDto> rows = notificationMapper.selectNotifications(memberId, offset, size);
        return new NotificationListResponse(rows, rows.size());
    }

    @Override
    @Transactional
    public void sendAndSave(Long memberId, String title, String body, String type, String imageUrl, String deeplink) {
        notificationTriggerService.saveAndPush(memberId, title, body, imageUrl, deeplink, type);
    }
}

