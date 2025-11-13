package cool.scx.scheduling;

import cool.scx.function.Function1Void;
import cool.scx.timer.ScxTimer;

import java.util.function.Consumer;

/// 调度任务
///
/// @author scx567888
/// @version 0.0.1
public interface ScheduleTask<T extends ScheduleTask<T>> {

    /// 设置 ScxTimer
    T timer(ScxTimer timer);

    /// 设置任务
    T task(Function1Void<TaskContext, ?> task);

    /// 设置错误处理器
    T onError(Consumer<Throwable> errorHandler);

    /// 启动任务
    ScheduleHandle start();

    /// 直接启动任务
    default ScheduleHandle start(Function1Void<TaskContext, ?> task) {
        return task(task).start();
    }

}
