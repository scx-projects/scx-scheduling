package cool.scx.scheduling.periodic;

import cool.scx.function.Function1Void;
import cool.scx.scheduling.ExpirationPolicy;
import cool.scx.scheduling.TaskContext;
import cool.scx.timer.ScxTimer;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static cool.scx.scheduling.ExpirationPolicy.IMMEDIATE_COMPENSATION;

/// AbstractPeriodicScheduleTask
///
/// @author scx567888
/// @version 0.0.1
public abstract class AbstractPeriodicScheduleTask implements PeriodicScheduleTask {

    protected Supplier<Instant> startTimeSupplier;
    protected Duration interval;
    protected long maxRunCount;
    protected ExpirationPolicy expirationPolicy;
    protected ScxTimer timer;
    protected Function1Void<TaskContext, ?> task;
    protected Consumer<Throwable> errorHandler;

    public AbstractPeriodicScheduleTask() {
        this.startTimeSupplier = null;
        this.interval = null;
        this.maxRunCount = -1;
        this.expirationPolicy = IMMEDIATE_COMPENSATION;
        this.timer = null;
        this.task = null;
        this.errorHandler = null;
    }

    @Override
    public PeriodicScheduleTask startTime(Supplier<Instant> startTimeSupplier) {
        this.startTimeSupplier = startTimeSupplier;
        return this;
    }

    @Override
    public PeriodicScheduleTask interval(Duration interval) {
        this.interval = interval;
        return this;
    }

    @Override
    public PeriodicScheduleTask maxRunCount(long maxRunCount) {
        this.maxRunCount = maxRunCount;
        return this;
    }

    @Override
    public PeriodicScheduleTask expirationPolicy(ExpirationPolicy expirationPolicy) {
        this.expirationPolicy = expirationPolicy;
        return this;
    }

    @Override
    public PeriodicScheduleTask timer(ScxTimer timer) {
        this.timer = timer;
        return this;
    }

    @Override
    public PeriodicScheduleTask task(Function1Void<TaskContext, ?> task) {
        this.task = task;
        return this;
    }

    @Override
    public PeriodicScheduleTask onError(Consumer<Throwable> errorHandler) {
        this.errorHandler = errorHandler;
        return this;
    }

}
