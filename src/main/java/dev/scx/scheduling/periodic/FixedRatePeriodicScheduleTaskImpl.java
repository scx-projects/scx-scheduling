package dev.scx.scheduling.periodic;

import dev.scx.scheduling.ScheduleHandle;
import dev.scx.scheduling.ScheduleStatus;
import dev.scx.scheduling.TaskContext;
import dev.scx.timer.ScxTimer;

import java.lang.System.Logger;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static dev.scx.scheduling.ScheduleStatus.*;
import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.getLogger;
import static java.time.Duration.between;
import static java.time.Instant.now;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

/// FixedRatePeriodicScheduleTaskImpl
///
/// @author scx567888
/// @version 0.0.1
public final class FixedRatePeriodicScheduleTaskImpl extends AbstractPeriodicScheduleTask {

    private static final Logger LOGGER = getLogger(FixedRatePeriodicScheduleTaskImpl.class.getName());

    private final AtomicLong runCount;
    private final AtomicBoolean cancel;

    private ScheduleHandle scheduleHandle;
    private Instant startTime;

    public FixedRatePeriodicScheduleTaskImpl(ScxTimer timer) {
        super(timer);
        this.runCount = new AtomicLong(0);
        this.cancel = new AtomicBoolean(false);
        this.scheduleHandle = null;
        this.startTime = null;
    }

    @Override
    public ScheduleHandle start() {
        // 0, 检查 参数
        if (task == null) {
            throw new IllegalStateException("task 未设置 !!!");
        }
        if (interval == null) {
            throw new IllegalStateException("interval 未设置 !!!");
        }

        // 1, 此处立即获取当前时间保证准确
        var now = now();

        // 2, 获取开始时间
        this.startTime = startTimeSupplier != null ? startTimeSupplier.get() : null;

        // 没有开始时间 就以当前时间为开始时间
        if (startTime == null) {
            startTime = now;
        }

        // 计算差值
        var diff = between(now, startTime);

        long startDelayNanos = diff.toNanos();

        // 如果过期 需要处理过期策略
        if (diff.isNegative()) {
            // 计算丢失了多少次执行次数
            var missCount = diff.dividedBy(interval) * -1;
            // 计算最近开始的时间
            var nearestTime = scheduledTimeOf(missCount + 1);
            // 以下处理过期情况
            switch (expirationPolicy) {
                case IMMEDIATE_IGNORE -> {
                    // 矫正 startTime
                    this.startTime = nearestTime;
                    // 使用 nearestTime 计算 startDelayNanos
                    startDelayNanos = between(now, nearestTime).toNanos();
                }
                case BACKTRACKING_IGNORE -> {
                    // 这里需要 "补账"
                    runCount.addAndGet(missCount);
                    // 矫正 startTime (让 startTime 延后一个 周期)
                    this.startTime = startTime.plus(interval);
                    // 使用 nearestTime 计算 startDelayNanos
                    startDelayNanos = between(now, nearestTime).toNanos();
                }
                case IMMEDIATE_COMPENSATION -> {
                    // 矫正 startTime
                    this.startTime = now;
                    // 立即执行.
                    startDelayNanos = 0;
                }
                case BACKTRACKING_COMPENSATION -> {
                    // 这里需要 "补偿运行", 无需矫正 startTime (因为 runCount 会增长)
                    for (var i = 0; i < missCount; i = i + 1) {
                        runTask(false);
                    }
                    // "补偿运行" 完 立即执行.
                    startDelayNanos = 0;
                }
            }
        }

        // 开启首次调度
        timer.runAfter(() -> runTask(true), startDelayNanos, NANOSECONDS);

        // 创建 ScheduleHandle
        this.scheduleHandle = new ScheduleHandle() {

            @Override
            public long runCount() {
                return runCount.get();
            }

            @Override
            public Instant nextRunTime() {
                if (cancel.get()) {
                    return null;
                }
                if (maxRunCount != -1 && runCount.get() >= maxRunCount) {
                    return null;
                }
                return scheduledTimeOf(runCount.get());
            }

            @Override
            public Instant nextRunTime(int count) {
                if (cancel.get()) {
                    return null;
                }
                if (maxRunCount != -1 && runCount.get() + count > maxRunCount) {
                    return null;
                }
                return scheduledTimeOf(runCount.get() + count);
            }

            @Override
            public void cancel() {
                cancel.set(true);
            }

            @Override
            public ScheduleStatus status() {
                if (cancel.get()) {
                    return CANCELLED;
                }
                if (maxRunCount != -1 && runCount.get() >= maxRunCount) {
                    return DONE;
                }
                return RUNNING;
            }

        };

        return scheduleHandle;
    }

    /// 计算从起始时间点开始, 第几次执行的时间
    private Instant scheduledTimeOf(long count) {
        return startTime.plus(interval.multipliedBy(count));
    }

    // 此处使用 cancel 标记来实现取消. 而不是真正使用 TaskHandle 取消任务.
    // 也就是说即使用户调用取消, 后续也会触发一次回调(但会在回调开头被拦截)
    // 采用这种方式的原因是, 保存 TaskHandle 的复杂度要比使用 cancel 标记的复杂度更高, 而且多余的这一次回调开销也很小(几乎可以忽略).
    private void runTask(boolean scheduleNext) {
        var l = runCount.incrementAndGet();
        // 已经取消了 或者 达到了最大次数
        if (cancel.get() || maxRunCount != -1 && l > maxRunCount) {
            return;
        }

        if (scheduleNext) {
            // 立即调用下一次
            timer.runAfter(() -> runTask(true), between(now(), scheduledTimeOf(l)).toNanos(), NANOSECONDS);
        }

        try {
            task.apply(new TaskContext() {

                @Override
                public long currentRunCount() {
                    return l;
                }

                @Override
                public ScheduleHandle scheduleHandle() {
                    // 这里有可能是 null, 假设 startDelay 为 0 时 有可能先调用 runTask 然后才有返回值
                    return scheduleHandle;
                }

            });
        } catch (Throwable e) {
            if (errorHandler != null) {
                try {
                    errorHandler.accept(e);
                } catch (Throwable ex) {
                    e.addSuppressed(ex);
                    LOGGER.log(ERROR, "errorHandler 发生错误 !!!", e);
                }
            } else {
                LOGGER.log(ERROR, "调度任务时发生错误 !!!", e);
            }
        }

    }

}
