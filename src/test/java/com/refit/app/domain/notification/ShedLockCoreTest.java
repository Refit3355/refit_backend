package com.refit.app.domain.notification;

import net.javacrumbs.shedlock.core.DefaultLockingTaskExecutor;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ShedLockCoreTest {

    @Autowired
    LockProvider lockProvider;

    @Test
    void onlyOneThreadExecutesTask() throws Exception {
        var executor = new DefaultLockingTaskExecutor(lockProvider);
        var counter = new AtomicInteger(0);

        Runnable tryRun = () -> executor.executeWithLock(
                (Runnable) () -> {
                    counter.incrementAndGet();
                    try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
                },
                new LockConfiguration(
                        Instant.now(),
                        "test-lock-core",          // 같은 락 이름
                        Duration.ofSeconds(5),     // lockAtMostFor
                        Duration.ofSeconds(3)      // lockAtLeastFor
                )
        );

        Thread t1 = new Thread(tryRun);
        Thread t2 = new Thread(tryRun);
        t1.start(); t2.start();
        t1.join(); t2.join();

        // 두 쓰레드가 동시에 시도했지만 실제 실행은 1번만
        assertThat(counter.get()).isEqualTo(1);
    }
}