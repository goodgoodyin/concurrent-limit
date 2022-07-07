package com.concurrentlimit.base.delayQueue;

/**
 * 自定义延迟补偿操作接口
 */
public interface DelayExecuteService {

    void delayExecute(Object obj);
}
