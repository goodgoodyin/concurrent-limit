package com.concurrentlimit.base.delayQueue;


import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 区间单位为s时的窗口坐标实现
 */
public class WindowPointSecond implements WindowPointService {

    private static final Logger LOGGER = LoggerFactory.getLogger(WindowPointSecond.class);

    private Integer sectionTime;

    public WindowPointSecond(int sectionTime) {
        this.sectionTime = sectionTime;
    }

    @Override
    public Integer getPoint() {
        if (sectionTime > 60 || sectionTime < 2) {
            LOGGER.error("sectionTime must greater 2 and less than 60");
            throw new RuntimeException("sectionTime must greater 2 and less than 60");
        }

        DateTime dateTime = DateTime.now();
        int second = dateTime.secondOfMinute().get();
        Integer windowPoint = second / sectionTime;
        return windowPoint;
    }

    @Override
    public Integer getSectionTime() {
        return sectionTime;
    }
}
