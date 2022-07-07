package com.concurrentlimit.base.delayQueue;

/**
 * 自定义限流后执行接口
 */
public interface LimitExecuteService {

    void limitExecute(Object obj);
}
