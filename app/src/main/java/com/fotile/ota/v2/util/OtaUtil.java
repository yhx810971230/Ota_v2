package com.fotile.ota.v2.util;

import java.math.BigDecimal;

public class OtaUtil {

    public static float getProgress(int progress, int max) {
        BigDecimal bigDecimal1 = new BigDecimal(progress);
        BigDecimal bigDecimal2 = new BigDecimal(max);
        return bigDecimal1.divide(bigDecimal2, 4, BigDecimal.ROUND_DOWN).floatValue();
    }

    public static String getFileName(String url){
        int index_start = url.lastIndexOf("/") + 1;
        int index_end = url.length();
        if(url.contains("?")){
            index_end = url.indexOf("?");
        }
        return url.substring(index_start, index_end);
    }
}
