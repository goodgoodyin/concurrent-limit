package com.concurrentlimit.base.delayQueue;


/**
 * 时间坐标为0
 */
public class NotGrabChatNotifyWinPoint implements WindowPointService{

    private Integer sectionTime;

    public NotGrabChatNotifyWinPoint(Integer sectionTime) {
        this.sectionTime = sectionTime;
    }

    @Override
    public Integer getPoint() {
        return 0;
    }

    @Override
    public Integer getSectionTime() {
        return this.sectionTime;
    }
}
