package com.concurrentlimit.base.delayQueue;


/**
 * 时间坐标
 */
public enum WindowPoint {

    /**
     * 单位为s时能被60整除的全部时间区间
     */
    SECOND_2(new WindowPointSecond(2)),
    SECOND_3(new WindowPointSecond(3)),
    SECOND_4(new WindowPointSecond(4)),
    SECOND_5(new WindowPointSecond(5)),
    SECOND_6(new WindowPointSecond(6)),
    SECOND_10(new WindowPointSecond(10)),
    SECOND_12(new WindowPointSecond(12)),
    SECOND_15(new WindowPointSecond(15)),
    SECOND_20(new WindowPointSecond(20)),
    SECOND_30(new WindowPointSecond(30)),

    /**
     * 单位为min时能被60整除的全部时间区间
     */
    MINUTE_2(new WindowPointMinute(2)),
    MINUTE_3(new WindowPointMinute(3)),
    MINUTE_4(new WindowPointMinute(4)),
    MINUTE_5(new WindowPointMinute(5)),
    MINUTE_6(new WindowPointMinute(6)),
    MINUTE_10(new WindowPointMinute(10)),
    MINUTE_12(new WindowPointMinute(12)),
    MINUTE_15(new WindowPointMinute(15)),
    MINUTE_20(new WindowPointMinute(20)),
    MINUTE_30(new WindowPointMinute(30)),

    /**
     * 自定义时间区间
     */
    NOT_GRAB_NOTIFY(new NotGrabChatNotifyWinPoint(60 * 60));
    ;

    WindowPointService pointService;

    WindowPoint(WindowPointService pointService) {
        this.pointService = pointService;
    }

    /**
     * 区间坐标
     * @return
     */
    public int getPoint(){
        return this.pointService.getPoint();
    }

    /**
     * 窗口区间
     * @return
     */
    public int getSectionTime(){
        return this.pointService.getSectionTime();
    }
}
