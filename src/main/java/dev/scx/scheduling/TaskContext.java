package dev.scx.scheduling;

/// 任务上下文
///
/// @author scx567888
/// @version 0.0.1
public interface TaskContext {

    /// 当前运行次数 (快照)
    long currentRunCount();

    /// 调度 Handle
    ScheduleHandle scheduleHandle();

    /// 取消调度
    default void cancelSchedule() {
        scheduleHandle().cancel();
    }

}
