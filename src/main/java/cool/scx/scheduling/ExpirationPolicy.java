package cool.scx.scheduling;

/// 过期策略
///
/// @author scx567888
/// @version 0.0.1
public enum ExpirationPolicy {

    /// 立即忽略
    /// - 单次任务 可以理解为不执行
    /// - 针对多次任务 会计算距离当前最近的调度时间点 之前的跳过 (runCount 不会增加)
    IMMEDIATE_IGNORE,

    /// 回溯忽略
    /// - 针对单次任务 可以理解为不执行
    /// - 针对多次任务 会计算距离当前最近的调度时间点 之前的跳过 (runCount 会增加)
    /// - 可以理解为之前的任务执行了 (实际并没有,被忽略了) 但是 runCount 还是会增加
    BACKTRACKING_IGNORE,

    /// 立即补偿
    /// - 针对单次任务 可以理解为立即执行
    /// - 针对多次任务 会立即执行一次 之后按照正常调度时间点处理
    IMMEDIATE_COMPENSATION,

    /// 回溯补偿
    /// - 针对单次任务 可以理解为立即执行 和 IMMEDIATE_COMPENSATION 相同
    /// - 针对多次任务 会计算未执行的次数 然后立即执行 之后按照正常调度时间点处理
    BACKTRACKING_COMPENSATION

}
