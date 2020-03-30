package com.fotile.ota.v2.bean;

import com.fotile.ota.v2.util.DownStatus;
import com.fotile.ota.v2.util.OtaUtil;

public class FileInfo {
    public FileInfo() {
    }

    public FileInfo(String fileName, String url) {
        this.file_name = fileName;
        url_pre = OtaUtil.getUrlPre(url);
        url_suf = OtaUtil.getUrlSuf(url);
    }

    /**
     * 文件名
     */
    public String file_name;
    /**
     * 下载地址 ?前面的字符串
     */
    public String url_pre = "";
    /**
     * 下载地址 ?后面的字符串
     */
    public String url_suf = "";
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

    public String getUrl(){
        return url_pre + url_suf;
    }

    @Override
    public String toString() {
        String result = "[file_name:" + file_name + "]"
                        + "[url:" + getUrl() + "]"
                        +" [length:" + length + "]"
                        + "[finished:" + finished + "]" ;
        return result;
    }
}
