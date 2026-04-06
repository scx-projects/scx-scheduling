package dev.scx.scheduling.one_time;

import dev.scx.scheduling.ExpirationPolicy;
import dev.scx.scheduling.ScheduleTask;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;

import static java.time.Instant.now;

/// OneTimeScheduleTask
///
/// @author scx567888
/// @version 0.0.1
public interface OneTimeScheduleTask extends ScheduleTask<OneTimeScheduleTask> {

    OneTimeScheduleTask startTime(Supplier<Instant> startTimeSupplier);

    OneTimeScheduleTask expirationPolicy(ExpirationPolicy expirationPolicy);

    default OneTimeScheduleTask startTime(Instant startTime) {
        return startTime(() -> startTime);
    }

    default OneTimeScheduleTask startDelay(Duration startDelay) {
        return startTime(() -> now().plus(startDelay));
    }

}
