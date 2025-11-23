package dev.scx.scheduling.periodic;

import dev.scx.scheduling.ExpirationPolicy;
import dev.scx.scheduling.ScheduleTask;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;

import static java.time.Instant.now;

/// PeriodicScheduleTask
///
/// @author scx567888
/// @version 0.0.1
public interface PeriodicScheduleTask extends ScheduleTask<PeriodicScheduleTask> {

    PeriodicScheduleTask startTime(Supplier<Instant> startTimeSupplier);

    PeriodicScheduleTask interval(Duration interval);

    PeriodicScheduleTask maxRunCount(long maxRunCount);

    PeriodicScheduleTask expirationPolicy(ExpirationPolicy expirationPolicy);

    default PeriodicScheduleTask startTime(Instant startTime) {
        return startTime(() -> startTime);
    }

    default PeriodicScheduleTask startDelay(Duration startDelay) {
        return startTime(() -> now().plus(startDelay));
    }

}
