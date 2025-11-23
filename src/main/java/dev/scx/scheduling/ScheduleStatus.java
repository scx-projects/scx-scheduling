package dev.scx.scheduling;

/// 调度状态 这表示 宏观的调度的状态 而不是 子任务的状态.
/// 比如在一个正在运行的调度的两个任务的间歇期间 仍会返回 RUNNING.
///
/// @author scx567888
/// @version 0.0.1
public enum ScheduleStatus {

    /// 运行中
    RUNNING,

    /// 已完成
    DONE,

    /// 已取消
    CANCELLED,

}
