package com.fotile.ota.v2;

import com.fotile.ota.v2.bean.FileInfo;

//下载进度接口
public interface OnProgressListener {
    void onDownLoading(FileInfo fileInfo);

    void onDownError(FileInfo fileInfo, String error);

    void onDownCompleted(FileInfo fileInfo);

    void onDownStop(FileInfo fileInfo);

}