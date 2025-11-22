package dev.scx.scheduling.one_time;

import dev.scx.function.Function1Void;
import dev.scx.scheduling.ExpirationPolicy;
import dev.scx.scheduling.ScheduleHandle;
import dev.scx.scheduling.ScheduleStatus;
import dev.scx.scheduling.TaskContext;
import dev.scx.timer.ScxTimer;
import dev.scx.timer.TaskStatus;

import java.lang.System.Logger;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static dev.scx.scheduling.ExpirationPolicy.IMMEDIATE_COMPENSATION;
import static dev.scx.scheduling.ScheduleStatus.*;
import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.getLogger;
import static java.time.Duration.between;
import static java.time.Instant.now;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

/// OneTimeScheduleTaskImpl
///
/// @author scx567888
/// @version 0.0.1
public final class OneTimeScheduleTaskImpl implements OneTimeScheduleTask {

    private static final Logger LOGGER = getLogger(OneTimeScheduleTaskImpl.class.getName());

    private final ScxTimer timer;

    private final AtomicLong runCount;

    private Supplier<Instant> startTimeSupplier;
    private ExpirationPolicy expirationPolicy;
    private Function1Void<TaskContext, ?> task;
    private Consumer<Throwable> errorHandler;

    private ScheduleHandle scheduleHandle;

    public OneTimeScheduleTaskImpl(ScxTimer timer) {
        if (timer == null) {
            throw new NullPointerException("timer 不允许为空 !!!");
        }
        this.timer = timer;
        this.runCount = new AtomicLong(0);
        this.startTimeSupplier = null;
        this.expirationPolicy = IMMEDIATE_COMPENSATION; // 默认过期补偿
        this.task = null;
        this.errorHandler = null;
        this.scheduleHandle = null;
    }

    @Override
    public OneTimeScheduleTask startTime(Supplier<Instant> startTimeSupplier) {
        this.startTimeSupplier = startTimeSupplier;
        return this;
    }

    @Override
    public OneTimeScheduleTask expirationPolicy(ExpirationPolicy expirationPolicy) {
        this.expirationPolicy = expirationPolicy;
        return this;
    }

    @Override
    public OneTimeScheduleTask task(Function1Void<TaskContext, ?> task) {
        this.task = task;
        return this;
    }

    @Override
    public OneTimeScheduleTask onError(Consumer<Throwable> errorHandler) {
        this.errorHandler = errorHandler;
        return this;
    }

    @Override
    public ScheduleHandle start() {
        // 0, 检查 参数
        if (task == null) {
            throw new IllegalStateException("task 未设置 !!!");
        }

        // 1, 此处立即获取当前时间保证准确
        var now = now();

        // 2, 获取开始时间
        var startTime = startTimeSupplier != null ? startTimeSupplier.get() : null;

        // 没有开始时间 就以当前时间为开始时间
        if (startTime == null) {
            startTime = now;
        }

        // 计算差值
        var diff = between(now, startTime);

        long startDelayNanos = diff.toNanos();

        // 如果过期 需要处理过期策略
        if (diff.isNegative()) {
            switch (expirationPolicy) {
                case IMMEDIATE_IGNORE -> {
                    // 单次任务 直接返回虚拟的 Status 即可 无需执行
                    return virtualScheduleHandle();
                }
                case BACKTRACKING_IGNORE -> {
                    // 这里需要 "补账"
                    runCount.incrementAndGet();
                    // 单次任务 直接返回虚拟的 Status 即可 无需执行
                    return virtualScheduleHandle();
                }
                // 单次任务的补偿策略就是立即执行
                case IMMEDIATE_COMPENSATION, BACKTRACKING_COMPENSATION -> {
                    startDelayNanos = 0;
                }
            }
        }

        // 计算任务的实际启动时间
        var firstRunTime = now.plusNanos(startDelayNanos);

        // 创建执行任务
        var taskHandle = timer.runAfter(this::runTask, startDelayNanos, NANOSECONDS);

        // 创建 ScheduleHandle
        this.scheduleHandle = new ScheduleHandle() {

            @Override
            public void cancel() {
                taskHandle.cancel();
            }

            @Override
            public ScheduleStatus status() {
                var taskStatus = taskHandle.status();
                return switch (taskStatus) {
                    case PENDING, RUNNING -> RUNNING;
                    case SUCCESS, FAILED -> DONE;
                    case CANCELLED -> CANCELLED;
                };
            }

            @Override
            public long runCount() {
                return runCount.get();
            }

            @Override
            public Instant nextRunTime() {
                // 只有没执行才有下一次的时间
                if (taskHandle.status() == TaskStatus.PENDING) {
                    return firstRunTime;
                }
                return null;
            }

            @Override
            public Instant nextRunTime(int count) {
                return count == 1 ? nextRunTime() : null;
            }

        };

        return scheduleHandle;
    }

    private ScheduleHandle virtualScheduleHandle() {
        return new ScheduleHandle() {

            @Override
            public long runCount() {
                return runCount.get();
            }

            @Override
            public Instant nextRunTime() {
                return null; // 忽略策略没有下一次运行时间
            }

            @Override
            public Instant nextRunTime(int count) {
                return null;// 同上
            }

            @Override
            public void cancel() {
                // 任务从未执行所以无需取消
            }

            @Override
            public ScheduleStatus status() {
                // 忽略了 所以总是完成
                return DONE;
            }

        };
    }

    private void runTask() {
        var l = runCount.incrementAndGet();

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
