package dev.scx.scheduling.test;

import dev.scx.scheduling.ScxScheduling;
import org.testng.annotations.Test;

import java.time.Duration;
import java.time.Instant;

import static dev.scx.scheduling.ExpirationPolicy.*;

public class ScxSchedulingTest {

    public static void main(String[] args) {
//        test1();
//        test2();
//        test3();
        test4();
    }

    public static void test1() {

        //测试单次

        //正常执行
        ScxScheduling
            .oneTime()
            .startTime(Instant.now().plusSeconds(1))
            .start((a) -> {
                System.err.println("这是通过 once() 0 打印的 !!! runCount : " + a.currentRunCount());
            });

        //过期执行 1
        ScxScheduling
            .oneTime()
            .startTime(Instant.now().minusSeconds(1))
            .expirationPolicy(BACKTRACKING_IGNORE)
            .start((a) -> {
                System.err.println("这是通过 once() 1 打印的 !!! 但因为被忽略了 所以不会打印 !!! ");
            });

        //过期执行 2
        ScxScheduling
            .oneTime()
            .startTime(Instant.now().minusSeconds(1))
            .expirationPolicy(BACKTRACKING_COMPENSATION)
            .start((a) -> {
                System.err.println("这是通过 once() 2 打印的 !!! 因为补偿策略所以会瞬间打印 runCount : " + a.currentRunCount());
            });

        //测试取消
        var status = ScxScheduling
            .oneTime()
            .startTime(Instant.now().plusSeconds(1))
            .start((a) -> {
                System.err.println("这是通过 once() 打印的 但是会被取消所以不会打印 !!!");
            });

        System.out.println(status.runCount());
        status.cancel();

    }

    public static void test2() {

        //测试 固定频率

        //正常执行
        ScxScheduling
            .fixedRate()
            .interval(Duration.ofMillis(200))
            .maxRunCount(10)
            .task((a) -> {
                System.err.println("这是通过 fixedRate() 0 打印的 : 第 10 次会取消 , 这时第 " + a.currentRunCount() + " 次执行 !!!");
            })
//            .start()
        ;

//        //过期策略 1
        ScxScheduling
            .fixedRate()
            .startTime(Instant.now().minusMillis(10000))
            .expirationPolicy(IMMEDIATE_IGNORE)
            .interval(Duration.ofMillis(500))
            .maxRunCount(10)
            .task((a) -> {
                System.err.println("这是通过 fixedRate() 1 打印的 : 第 10 次会取消 , 这时第 " + a.currentRunCount() + " 次执行 !!!");
            })
//            .start()
        ;

        //过期策略 2
        ScxScheduling
            .fixedRate()
            .startTime(Instant.now().minusMillis(1000))
            .expirationPolicy(BACKTRACKING_IGNORE)
            .interval(Duration.ofMillis(500))
            .maxRunCount(10)
            .task((a) -> {
                System.err.println("这是通过 fixedRate() 2 打印的 : 第 10 次会取消 , 这时第 " + a.currentRunCount() + " 次执行 !!!");
            })
//            .start()
        ;

        //过期策略 3
        ScxScheduling
            .fixedRate()
            .startTime(Instant.now().minusMillis(1000))
            .expirationPolicy(IMMEDIATE_COMPENSATION)
            .interval(Duration.ofMillis(500))
            .maxRunCount(10)
            .task((a) -> {
                System.err.println("这是通过 fixedRate() 3 打印的 : 第 10 次会取消 , 这时第 " + a.currentRunCount() + " 次执行 !!!");
            })
//            .start()
        ;

        //过期策略 3
        ScxScheduling
            .fixedRate()
            .startTime(Instant.now().minusMillis(1000))
            .expirationPolicy(BACKTRACKING_COMPENSATION)
            .interval(Duration.ofMillis(500))
            .maxRunCount(10)
            .task((a) -> {
                System.err.println("这是通过 fixedRate() 3 打印的 : 第 10 次会取消 , 这时第 " + a.currentRunCount() + " 次执行 !!!");
            })
            .start()
        ;

    }

    public static void test3() {

        ScxScheduling
            .cron()
            .cronExpression("*/1 * * * * ?")
            .maxRunCount(3)
            .start((a) -> {
                //测试
                System.err.println("这是使用 Cron 表达式 打印的 : 这是第 " + a.currentRunCount() + " 次执行 !!!");
            });

    }

    @Test
    public static void test4() {
        ScxScheduling.fixedRate().interval(Duration.ofMillis(200)).maxRunCount(10).start((context) -> {
            System.out.println("第 " + context.currentRunCount() + " 次运行 !!!");
            if (context.currentRunCount() == 10) {
                System.out.println("第 10 次了, 关闭线程池 !!!");
                ScxScheduling.shutdownDefaultTimer();
            }
        });
        System.out.println("关闭了 !!!");
    }

}
