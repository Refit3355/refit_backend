package com.refit.app.domain.order.scheduler;

import com.refit.app.domain.notification.service.NotificationTriggerService;
import com.refit.app.domain.order.dto.AutoConfirmTarget;
import com.refit.app.domain.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AutoConfirmScheduler {

    private final OrderService orderService;
    private final NotificationTriggerService notificationTriggerService;

    // 매일 08:30 KST 실행
    @Scheduled(cron = "0 30 8 * * *", zone = "Asia/Seoul")
    @SchedulerLock(name = "auto-confirm-delivered-5days", lockAtMostFor = "PT5M", lockAtLeastFor = "PT30S")
    @Transactional
    public void autoConfirm() {
        // 대상 잠금 + 수집 + 상태 전환
        List<AutoConfirmTarget> targets = orderService.collectTargetsForAutoConfirm();

        // 알림 발송
        int n = 0;
        for (AutoConfirmTarget t : targets) {
            try {
                String body = String.format("'%s' 상품이 배송완료 후 5일 경과되어 자동으로 구매가 확정되었습니다.",
                        t.getBrandName() + " " + t.getProductName());
                notificationTriggerService.notifyOrderAutoConfirmed(t.getMemberId(), t.getOrderItemId(), body);
                n++;
            } catch (Exception e) {
                log.warn("auto-confirm notify failed: memberId={}, orderItemId={}, err={}",
                        t.getMemberId(), t.getOrderItemId(), e.toString());
            }
        }
        log.info("Auto confirm done. updated={}, notified={}", targets.size(), n);
    }
}