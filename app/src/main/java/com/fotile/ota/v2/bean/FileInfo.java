package com.fotile.ota.v2.bean;

import com.fotile.ota.v2.util.DownStatus;

public class FileInfo {
    public FileInfo() {
    }

    public FileInfo(String fileName, String url) {
        this.fileName = fileName;
        this.url = url;
    }

    /**
     * 文件名
     */
    public String fileName;
    /**
     * 下载地址
     */
    public String url;
    /**
     * 文件大小
     */
    public int length;
    /**
     * 下载已完成进度
     */
    public int finished;

    public DownStatus status = DownStatus.DOWN_STATUS_INIT;

//    /**
//     * 是否暂停下载
//     */
//    public boolean isStop = false;
//    /**
//     * 是否正在下载
//     */
//    public boolean isDownLoading = false;

    @Override
    public String toString() {
        String result = "[fileName:" + fileName + "]"
                        + "[url:" + url + "]"
                        +" [length:" + length + "]"
                        + "[finished:" + finished + "]" ;
        return result;
    }
}
