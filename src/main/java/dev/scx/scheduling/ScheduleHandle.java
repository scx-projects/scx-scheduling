package dev.scx.scheduling;

import java.time.Instant;

/// ScheduleHandle
///
/// @author scx567888
/// @version 0.0.1
public interface ScheduleHandle {

    /// 取消调度, 不包括已经开始的子任务.
    void cancel();

    /// 调度器状态
    ScheduleStatus status();

    /// 子任务运行的次数 会动态变化
    long runCount();

    /// 预计下一次子任务运行的时间, 如果下一次不会运行任何子任务 则返回 null
    Instant nextRunTime();

    /// 预计指定次数后子任务运行的时间, 假设 当前调度器拥有 周期次数 限制 那么当超出限制之后 会返回 null
    Instant nextRunTime(int count);

}
