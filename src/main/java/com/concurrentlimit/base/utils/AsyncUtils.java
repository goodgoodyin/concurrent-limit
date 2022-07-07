package com.concurrentlimit.base.utils;


import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 异步任务执行
 */
public class AsyncUtils {

    /**
     * <br>异步任务提交后，都是在这个 链式 阻塞队列里 存着
     */
    private static final LinkedBlockingQueue<Runnable> TASK_WAITING_QUEUE = new LinkedBlockingQueue<>();


    /**
     * <br>定长线程池，目前大小为 10
     * <br>数量暂时写死，没有想到什么好的办法支持业务方指定
     */
    private static final ThreadPoolExecutor EXECUTOR_SERVICE =
            new ThreadPoolExecutor(10,10, 0L, TimeUnit.MILLISECONDS, TASK_WAITING_QUEUE);

    public static void run(Runnable task){
        EXECUTOR_SERVICE.execute(task);
    }

}
