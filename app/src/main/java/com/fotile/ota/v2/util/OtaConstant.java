package com.fotile.ota.v2.util;

import android.os.Environment;

public class OtaConstant {

    /**
     * 文件下载保存路径
     */
    public static String FILE_PATH = Environment.getExternalStorageDirectory().getPath() + "/fotile/ota/";
    /**
     * 未完成下载文件后缀名
     */
    public static String TEMP_FILE_NAME = ".tmp";
}
