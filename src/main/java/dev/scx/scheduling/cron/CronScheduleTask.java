package dev.scx.scheduling.cron;

import dev.scx.scheduling.ScheduleTask;

/// CronScheduleTask
///
/// @author scx567888
public interface CronScheduleTask extends ScheduleTask<CronScheduleTask> {

    CronScheduleTask cronExpression(String cronExpression) throws IllegalArgumentException;

    CronScheduleTask maxRunCount(long maxRunCount);

}
