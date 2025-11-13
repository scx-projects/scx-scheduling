package cool.scx.scheduling.cron;

import cool.scx.scheduling.ScheduleTask;

/// CronScheduleTask
///
/// @author scx567888
/// @version 0.0.1
public interface CronScheduleTask extends ScheduleTask<CronScheduleTask> {

    CronScheduleTask cronExpression(String cronExpression) throws IllegalArgumentException;

    CronScheduleTask maxRunCount(long maxRunCount);

}
