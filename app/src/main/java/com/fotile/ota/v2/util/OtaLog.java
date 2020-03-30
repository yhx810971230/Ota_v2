package com.fotile.ota.v2.util;

import android.util.Log;

public class OtaLog {
    /**
     * 获取isDebug存储值
     */
    public static boolean isDebug = true;

    public static void LOGE(String tag, Object obj) {
        if (isDebug) {
            if (null != obj) {
                Log.e("Ota" + tag, obj.toString());
            } else {
                Log.e("Ota" + tag, "null");
            }
        }
    }
}
