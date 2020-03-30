package com.fotile.ota.v2.util;

public enum DownStatus {
    //初始状态，表示文件还未开始下载
    DOWN_STATUS_INIT("down_status_init"),
    //下载中
    DOWN_STATUS_DOWNING("down_status_downing"),
    //下载暂停
    DOWN_STATUS_STOP("down_status_stop"),
    //文件下载完成
    DOWN_STATUS_COMPLETED("down_status_completed"),
    //文件下载错误
    DOWN_STATUS_ERROR("down_status_error");

    private String value = "";

    DownStatus(String value){
        this.value = value;
    }

    public String getValue(){
        return value;
    }

    public static DownStatus getDownStatus(String value){
        if("down_status_init".equals(value)){
            return DOWN_STATUS_INIT;
        }
        if("down_status_downing".equals(value)){
            return DOWN_STATUS_DOWNING;
        }
        if("down_status_stop".equals(value)){
            return DOWN_STATUS_STOP;
        }
        if("down_status_completed".equals(value)){
            return DOWN_STATUS_COMPLETED;
        }
        if("down_status_error".equals(value)){
            return DOWN_STATUS_ERROR;
        }
        return DOWN_STATUS_INIT;
    }
}
