package dev.scx.scheduling.cron;

import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import dev.scx.function.Function1Void;
import dev.scx.scheduling.ScheduleHandle;
import dev.scx.scheduling.ScheduleStatus;
import dev.scx.scheduling.TaskContext;
import dev.scx.timer.ScxTimer;

import java.lang.System.Logger;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import static com.cronutils.model.CronType.QUARTZ;
import static com.cronutils.model.definition.CronDefinitionBuilder.instanceDefinitionFor;
import static dev.scx.scheduling.ScheduleStatus.*;
import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.getLogger;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

/// CronScheduleTaskImpl
///
/// @author scx567888
/// @version 0.0.1
public final class CronScheduleTaskImpl implements CronScheduleTask {

    private static final Logger LOGGER = getLogger(CronScheduleTaskImpl.class.getName());

    // 这里默认用 QUARTZ 的格式
    private static final CronParser CRON_PARSER = new CronParser(instanceDefinitionFor(QUARTZ));

    private final ScxTimer timer;

    private final AtomicLong runCount;
    private final AtomicBoolean cancel;

    private ExecutionTime cronExecutionTime;
    private long maxRunCount;
    private Function1Void<TaskContext, ?> task;
    private Consumer<Throwable> errorHandler;

    private ScheduleHandle scheduleHandle;
    private ZonedDateTime nextExecutionTime;

    public CronScheduleTaskImpl(ScxTimer timer) {
        if (timer == null) {
            throw new NullPointerException("timer 不允许为空 !!!");
        }
        this.timer = timer;
        this.runCount = new AtomicLong(0);
        this.cancel = new AtomicBoolean(false);
        this.cronExecutionTime = null;
        this.maxRunCount = -1;
        this.task = null;
        this.errorHandler = null;
        this.scheduleHandle = null;
    }

    @Override
    public CronScheduleTask cronExpression(String cronExpression) throws IllegalArgumentException {
        var cron = CRON_PARSER.parse(cronExpression);
        this.cronExecutionTime = ExecutionTime.forCron(cron);
        return this;
    }

    @Override
    public CronScheduleTask maxRunCount(long maxRunCount) {
        this.maxRunCount = maxRunCount;
        return this;
    }

    @Override
    public CronScheduleTask task(Function1Void<TaskContext, ?> task) {
        this.task = task;
        return this;
    }

    @Override
    public CronScheduleTask onError(Consumer<Throwable> errorHandler) {
        this.errorHandler = errorHandler;
        return this;
    }

    @Override
    public ScheduleHandle start() {
        // 0, 检查 参数
        if (task == null) {
            throw new IllegalStateException("task 未设置 !!!");
        }
        if (cronExecutionTime == null) {
            throw new IllegalStateException("cronExecution 未设置 !!!");
        }

        // 开启调度
        scheduleNext();

        // 创建 ScheduleHandle
        this.scheduleHandle = new ScheduleHandle() {

            @Override
            public long runCount() {
                return runCount.get();
            }

            @Override
            public Instant nextRunTime() {
                return nextExecutionTime != null ? nextExecutionTime.toInstant() : null;
            }

            @Override
            public Instant nextRunTime(int count) {
                ZonedDateTime nextTime = nextExecutionTime;
                for (int i = 0; i < count; i = i + 1) {
                    nextTime = cronExecutionTime.nextExecution(nextTime).orElse(null);
                }
                return nextTime != null ? nextTime.toInstant() : null;
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

    private void scheduleNext() {

        var now = ZonedDateTime.now();

        if (nextExecutionTime == null) {
            nextExecutionTime = now;
        }

        nextExecutionTime = cronExecutionTime.nextExecution(nextExecutionTime).orElse(null);

        if (nextExecutionTime == null) {
            // 没有下一次执行时间，停止调度 这种情况很难发生
            return;
        }

        var startDelayNanos = Duration.between(now, nextExecutionTime).toNanos();

        // 此处我们不使用返回的 TaskHandle 来控制取消.
        timer.runAfter(this::runTask, startDelayNanos, NANOSECONDS);

    }

    // 此处使用 cancel 标记来实现取消. 而不是真正使用 TaskHandle 取消任务.
    // 也就是说即使用户调用取消, 后续也会触发一次回调(但会在回调开头被拦截)
    // 采用这种方式的原因是, 保存 TaskHandle 的复杂度要比使用 cancel 标记的复杂度更高, 而且多余的这一次回调开销也很小(几乎可以忽略).
    private void runTask() {
        var l = runCount.incrementAndGet();
        // 已经取消了 或者 达到了最大次数
        if (cancel.get() || maxRunCount != -1 && l > maxRunCount) {
            return;
        }

        // 递归调用下一次
        scheduleNext();

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
