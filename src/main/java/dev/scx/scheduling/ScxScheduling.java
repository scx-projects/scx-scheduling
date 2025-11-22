package dev.scx.scheduling;

import dev.scx.scheduling.cron.CronScheduleTask;
import dev.scx.scheduling.cron.CronScheduleTaskImpl;
import dev.scx.scheduling.one_time.OneTimeScheduleTask;
import dev.scx.scheduling.one_time.OneTimeScheduleTaskImpl;
import dev.scx.scheduling.periodic.FixedDelayPeriodicScheduleTaskImpl;
import dev.scx.scheduling.periodic.FixedRatePeriodicScheduleTaskImpl;
import dev.scx.scheduling.periodic.PeriodicScheduleTask;
import dev.scx.timer.ScheduledExecutorTimer;
import dev.scx.timer.ScxTimer;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.locks.ReentrantLock;

/// ScxScheduling
///
/// 用来 快捷的 创建调度任务
///
/// @author scx567888
/// @version 0.0.1
public final class ScxScheduling {

    private static final ReentrantLock defaultTimerLock = new ReentrantLock();
    private static ScheduledExecutorService defaultScheduledExecutorService;
    private static ScxTimer defaultTimer;

    public static ScxTimer defaultTimer() {
        defaultTimerLock.lock();
        try {
            if (defaultTimer == null) {
                defaultScheduledExecutorService = new ScheduledThreadPoolExecutor(Runtime.getRuntime().availableProcessors() * 2);
                defaultTimer = new ScheduledExecutorTimer(defaultScheduledExecutorService);
            }
            return defaultTimer;
        } finally {
            defaultTimerLock.unlock();
        }
    }

    public static void shutdownDefaultTimer() {
        defaultTimerLock.lock();
        try {
            if (defaultTimer != null) {
                defaultScheduledExecutorService.shutdown();
                defaultScheduledExecutorService = null;
                defaultTimer = null;
            }
        } finally {
            defaultTimerLock.unlock();
        }
    }

    public static OneTimeScheduleTask oneTime() {
        return oneTime(defaultTimer());
    }

    public static CronScheduleTask cron() {
        return cron(defaultTimer());
    }

    public static PeriodicScheduleTask fixedRate() {
        return fixedRate(defaultTimer());
    }

    public static PeriodicScheduleTask fixedDelay() {
        return fixedDelay(defaultTimer());
    }

    public static OneTimeScheduleTask oneTime(ScxTimer timer) {
        return new OneTimeScheduleTaskImpl(timer);
    }

    public static CronScheduleTask cron(ScxTimer timer) {
        return new CronScheduleTaskImpl(timer);
    }

    public static PeriodicScheduleTask fixedRate(ScxTimer timer) {
        return new FixedRatePeriodicScheduleTaskImpl(timer);
    }

    public static PeriodicScheduleTask fixedDelay(ScxTimer timer) {
        return new FixedDelayPeriodicScheduleTaskImpl(timer);
    }

    public static ScheduleHandle setTimeout(Runnable task, long delay) {
        return oneTime().startDelay(Duration.ofMillis(delay)).start((c) -> task.run());
    }

    public static ScheduleHandle setInterval(Runnable task, long delay) {
        return fixedRate().interval(Duration.ofMillis(delay)).start((c) -> task.run());
    }

}
