package com.refit.app.domain.notification.mapper;

import com.refit.app.domain.notification.dto.NotificationRowDto;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface NotificationMapper {
    int insertNotification(@Param("notificationId") Long notificationId,
            @Param("memberId") Long memberId,
            @Param("title") String title,
            @Param("body") String body,
            @Param("imageUrl") String imageUrl,
            @Param("deeplink") String deeplink,
            @Param("type") String type);

    int selectUnreadCount(@Param("memberId") Long memberId);

    int markAllRead(@Param("memberId") Long memberId);

    List<NotificationRowDto> selectNotifications(@Param("memberId") Long memberId,
            @Param("offset") int offset,
            @Param("size") int size);
}
