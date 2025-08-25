package com.refit.app.domain.notification.dto.response;

import com.refit.app.domain.notification.dto.NotificationRowDto;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class NotificationListResponse {
    private List<NotificationRowDto> items;
    private Integer size;
}
