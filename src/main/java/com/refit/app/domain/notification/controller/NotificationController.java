package com.refit.app.domain.notification.controller;

import com.refit.app.domain.notification.dto.request.RegisterDeviceRequest;
import com.refit.app.domain.notification.dto.request.SendNotificationRequest;
import com.refit.app.domain.notification.dto.response.BadgeResponse;
import com.refit.app.domain.notification.dto.response.BaseResponse;
import com.refit.app.domain.notification.dto.response.NotificationListResponse;
import com.refit.app.domain.notification.service.DeviceService;
import com.refit.app.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Validated
public class NotificationController {

    private final NotificationService notificationService;
    private final DeviceService deviceService;

    // 디바이스 토큰 등록
    @PostMapping("/devices/register")
    public ResponseEntity<BaseResponse> registerDevice(
            @Valid @RequestBody RegisterDeviceRequest req,
            @AuthenticationPrincipal Long memberId
    ) {
        deviceService.register(memberId, req.getPlatform(), req.getFcmToken(), req.getDeviceId());
        return ResponseEntity.ok(BaseResponse.ok("REGISTERED"));
    }

    // 토큰 삭제(로그아웃 시)
    @DeleteMapping("/devices/token")
    public ResponseEntity<BaseResponse> deleteToken(
            @RequestParam("deviceId") String deviceId,
            @AuthenticationPrincipal Long memberId
    ) {
        deviceService.deleteByDeviceId(memberId, deviceId);
        return ResponseEntity.ok(BaseResponse.ok("DELETED"));
    }

    // 배지 카운트
    @GetMapping("/badge")
    public ResponseEntity<BadgeResponse> badge(@AuthenticationPrincipal Long memberId) {
        int unread = notificationService.getUnreadCount(memberId);
        return ResponseEntity.ok(new BadgeResponse(unread));
    }

    // 전체 읽음 처리 (알림 페이지 입장 시)
    @PostMapping("/read-all")
    public ResponseEntity<BaseResponse> readAll(@AuthenticationPrincipal Long memberId) {
        int updated = notificationService.markAllRead(memberId);
        return ResponseEntity.ok(BaseResponse.ok("UPDATED:" + updated));
    }

    // 알림 목록
    @GetMapping("")
    public ResponseEntity<NotificationListResponse> list(
            @RequestParam(name="offset", defaultValue = "0") int offset,
            @RequestParam(name="size", defaultValue = "20") int size,
            @AuthenticationPrincipal Long memberId
    ) {
        NotificationListResponse body = notificationService.getList(memberId, offset, size);
        return ResponseEntity.ok(body);
    }

    // (관리/내부) 알림 생성 + 푸시 발송
    @PostMapping("/send")
    public ResponseEntity<BaseResponse> send(@Valid @RequestBody SendNotificationRequest req) {
        notificationService.sendAndSave(
                req.getMemberId(),
                req.getTitle(),
                req.getBody(),
                req.getType(),
                req.getImageUrl(),
                req.getDeeplink()
        );
        return ResponseEntity.ok(BaseResponse.ok("SENT"));
    }
}
