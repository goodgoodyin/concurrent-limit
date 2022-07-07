package com.concurrentlimit.base.delayQueue;

/**
 * 限流窗口坐标计算接口
 */
public interface WindowPointService {

    /**
     * 区间坐标
     * @return
     */
    Integer getPoint();

    /**
     * 区间时间(单位:s)
     * @return
     */
    Integer getSectionTime();

}
