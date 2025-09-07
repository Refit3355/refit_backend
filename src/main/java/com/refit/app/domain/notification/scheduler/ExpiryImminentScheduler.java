package com.refit.app.domain.notification.scheduler;

import com.refit.app.domain.memberProduct.dto.ExpiryCandidate;
import com.refit.app.domain.memberProduct.mapper.MemberProductMapper;
import com.refit.app.domain.notification.service.NotificationTriggerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExpiryImminentScheduler {

    private final MemberProductMapper memberProductMapper;
    private final NotificationTriggerService notificationTriggerService;


    // 매일 09:30 KST 실행
    @Scheduled(cron = "0 30 9 * * *", zone = "Asia/Seoul")
    @SchedulerLock(name = "expiry-imminent-7days-job", lockAtMostFor = "PT5M", lockAtLeastFor = "PT30S")
    @Transactional
    public void pushForExpiryIn7Days() {
        List<ExpiryCandidate> targets = memberProductMapper.selectExpiryIn7Days();
        log.info("[D-7] candidates={}", targets.size());

        for (ExpiryCandidate it : targets) {
            String body = String.format("'%s'의 소비기한이 7일 이하 남았습니다.", it.getProductName());
            try {
                notificationTriggerService.notifyExpiryImminent(it.getMemberId(), it.getMemberProductId(), body);
                memberProductMapper.markExpiry7Sent(it.getMemberProductId()); // 중복발송 방지 플래그
            } catch (Exception e) {
                log.warn("D-7 push failed mpId={}, memberId={}, err={}",
                        it.getMemberProductId(), it.getMemberId(), e.toString());
                // 실패 시 플래그 미변경 → 다음 실행 때 재시도
            }
        }
    }
}
