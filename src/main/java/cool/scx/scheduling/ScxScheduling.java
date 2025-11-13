package cool.scx.scheduling;

import cool.scx.scheduling.cron.CronScheduleTask;
import cool.scx.scheduling.cron.CronScheduleTaskImpl;
import cool.scx.scheduling.one_time.OneTimeScheduleTask;
import cool.scx.scheduling.one_time.OneTimeScheduleTaskImpl;
import cool.scx.scheduling.periodic.FixedDelayPeriodicScheduleTaskImpl;
import cool.scx.scheduling.periodic.FixedRatePeriodicScheduleTaskImpl;
import cool.scx.scheduling.periodic.PeriodicScheduleTask;
import cool.scx.timer.ScheduledExecutorTimer;
import cool.scx.timer.ScxTimer;

import java.time.Duration;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/// ScxScheduling
///
/// 可用来创建 快捷调度任务
///
/// @author scx567888
/// @version 0.0.1
public final class ScxScheduling {

    private static ScxTimer defaultTimer;

    public static ScxTimer defaultTimer() {
        if (defaultTimer == null) {
            defaultTimer = new ScheduledExecutorTimer(new ScheduledThreadPoolExecutor(Runtime.getRuntime().availableProcessors() * 2));
        }
        return defaultTimer;
    }

    public static OneTimeScheduleTask oneTime() {
        return new OneTimeScheduleTaskImpl().timer(defaultTimer());
    }

    public static CronScheduleTask cron() {
        return new CronScheduleTaskImpl().timer(defaultTimer());
    }

    public static PeriodicScheduleTask fixedRate() {
        return new FixedRatePeriodicScheduleTaskImpl().timer(defaultTimer());
    }

    public static PeriodicScheduleTask fixedDelay() {
        return new FixedDelayPeriodicScheduleTaskImpl().timer(defaultTimer());
    }

    public static ScheduleHandle setTimeout(Runnable task, long delay) {
        return oneTime().startDelay(Duration.ofMillis(delay)).start((c) -> task.run());
    }

    public static ScheduleHandle setInterval(Runnable task, long delay) {
        return fixedRate().interval(Duration.ofMillis(delay)).start((c) -> task.run());
    }

}
