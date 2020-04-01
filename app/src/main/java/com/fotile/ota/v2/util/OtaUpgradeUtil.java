package com.fotile.ota.v2.util;

import android.text.TextUtils;
import android.util.Base64;


import com.fotile.ota.v2.bean.UpgradeInfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Iterator;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;


/**
 * 文件名称：OtaUpgradeUtil
 * 创建时间：2017-10-09
 * 文件作者：fuzya
 * 功能描述：ota升级工具类
 */
public class OtaUpgradeUtil {
    /**
     * 正式环境
     */
    private static final String SERVER_URL_ONLINE = "http://ota.fotile.com:8080/fotileAdminSystem/upgrade.do?";
    /**
     * 测试-开发环境
     */
    private static final String SERVER_URL_TEST = "http://develop.fotile.com:8080/fotileAdminSystem/authenticationUpgrade.do?";
    private static final char HEX_DIGITS[] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D',
            'E', 'F'};

    public OtaUpgradeUtil() {

    }

    /**
     * 获取系统升级url
     *
     * @param packageName    应用包名
     * @param currentVersion 固件版本号
     * @return
     */
    public static String buildUrl(String packageName, String currentVersion) {
//        EnginBean enginBean = EnginUtil.getEnginBean();
        String url = SERVER_URL_ONLINE;
//        //开发环境
//        if(enginBean.ota_url == EnginType.ENGIN_URL_DEVELOP){
//
//        }
//        //测试环境
//        if(enginBean.ota_url == EnginType.ENGIN_URL_TEST){
//            url = SERVER_URL_TEST;
//        }

        url = SERVER_URL_TEST;
        StringBuilder sb = new StringBuilder(url);
        sb.append("package=");
        sb.append(packageName);
        sb.append('&');
        sb.append("version=");
        sb.append(currentVersion);
        sb.append("&isNew=1&isEncryption=0");
        return sb.toString();
    }


    /**
     * 请求OTA服务器
     */
    public static String httpGet(String strUrl, Map<String, String> heads) throws IOException {
        String result = null;
        URL url = new URL(strUrl);
        URLConnection urlConn = url.openConnection();
        HttpURLConnection connection = (HttpURLConnection) urlConn;
        connection.setConnectTimeout(15000);
        connection.setRequestMethod("GET");
        connection.setDoOutput(false);
        connection.setDoInput(true);
        connection.setUseCaches(false);
        connection.setInstanceFollowRedirects(true);
        //添加请求头
        if (null != heads && heads.size() > 0) {
            Iterator<String> iterator = heads.keySet().iterator();
            while (iterator.hasNext()) {
                String key = iterator.next();
                String value = heads.get(key);
                connection.setRequestProperty(key, value);
                OtaLog.LOGE("请求Ota head", key + " " + value);
            }
        }

        InputStream is = connection.getInputStream();

        // 从响应中获取长度
        int length = connection.getContentLength();

        if (length != -1) {
            byte[] data = new byte[length];
            byte[] temp = new byte[512];// 每次读取512字节
            int readLen = 0;// 单次读取的长度
            int destPos = 0;// 总字节数
            while ((readLen = is.read(temp)) > 0) {
                System.arraycopy(temp, 0, data, destPos, readLen);
                destPos += readLen;
            }
            result = new String(data, "UTF-8"); // 响应也是UTF-8编码
        }
        return result;
    }

    /**
     * DES解密操作
     *
     * @param src      密文
     * @param password 密钥
     * @return 明文
     * @throws Exception 异常
     */
    public static String Decrypt(String src, String password) throws Exception {
        byte[] ss = Base64.decode(src, Base64.DEFAULT);

        /* DES算法要求有一个可信任的随机数源 */
        SecureRandom random = new SecureRandom();
        /* 创建一个DESKeySpec对象 */
        DESKeySpec desKey = new DESKeySpec(password.getBytes());
        /* 创建一个密匙工厂 */
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
        /* 将DESKeySpec对象转换成SecretKey对象 */
        SecretKey securekey = keyFactory.generateSecret(desKey);
        /* Cipher对象实际完成解密操作 */
        Cipher cipher = Cipher.getInstance("DES");
        /* 用密匙初始化Cipher对象 */
        cipher.init(Cipher.DECRYPT_MODE, securekey, random);
        /* 真正开始解密操作 */
        byte[] dec = cipher.doFinal(ss);
        return new String(dec, "UTF-8");
    }

    /**
     * 生成文件的md5用于校验
     *
     * @param filename 文件名称
     * @return 文件md5
     */
    public static String md5sum(String filename) {
        InputStream fis;
        byte[] buffer = new byte[1024];
        int numRead = 0;
        MessageDigest md5;
        try {
            fis = new FileInputStream(filename);
            md5 = MessageDigest.getInstance("MD5");
            while ((numRead = fis.read(buffer)) > 0) {
                md5.update(buffer, 0, numRead);
            }
            fis.close();
            return toHexString(md5.digest());
        } catch (Exception e) {
            System.out.println("error");
            return "error";//这里不返回null，防止出现空指针
        }
    }

    /**
     * 十六进制转换
     *
     * @param b 数字流
     * @return 转换成的16进制
     */
    private static String toHexString(byte[] b) {
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (int i = 0; i < b.length; i++) {
            sb.append(HEX_DIGITS[(b[i] & 0xf0) >>> 4]);
            sb.append(HEX_DIGITS[b[i] & 0x0f]);
        }
        return sb.toString();
    }

    /**
     * 根据md5校验网络上的下载包，在本地是否存在
     * 该计算比较耗时
     *
     * @return
     */
    public static boolean exitOtaFile(UpgradeInfo mInfo) {
        if (null == mInfo) {
            return false;
        }
        //判断ota文件是否完整下载过了
        boolean exist_ota = false;
        //获取对应的下载文件
        File file_ota = new File(OtaConstant.FILE_PATH + OtaUtil.getFileName(mInfo.url));
        if (file_ota.exists()) {
            //本地文件md5
            String local_file_md5 = md5sum(file_ota.getPath());
            OtaLog.LOGE("md5校验", "网络md5=" + mInfo.md5 + "     本地md5=" + local_file_md5);
            if (!TextUtils.isEmpty(local_file_md5) && local_file_md5.equals(mInfo.md5)) {
                exist_ota = true;
            }
        }
        return exist_ota;
    }
//
//    public static boolean exitMcuFile(UpgradeInfo mInfo) {
//        if (null == mInfo) {
//            return false;
//        }
//        //判断mcu文件是否完整下载过了
//        boolean exis = false;
//        File file = new File(OtaConstant.FILE_ABSOLUTE_MCU);
//        if (file.exists()) {
//            //本地文件md5
//            String local_file_md5 = OtaUpgradeUtil.md5sum(file.getPath());
//            if (!TextUtils.isEmpty(local_file_md5) && local_file_md5.equals(mInfo.ex_md5)) {
//                exis = true;
//            }
//        }
//        return exis;
//    }

//    /**
//     * 更新存储在本地的OtaData文件信息
//     *
//     * @param otaData
//     */
//    public static void updateOtaData(OtaData otaData) {
//        File folder = new File(OtaConstant.FILE_FOLDER);
//        if (null != otaData && folder.exists()) {
//            File file = new File(OtaConstant.FILE_FOLDER + "otadata.txt");
//            //如果文件存在，先删除
//            if (file.exists()) {
//                file.delete();
//            }
//
//            //重新创建新文件
//            try {
//                file.createNewFile();
//                FileOutputStream outputStream = new FileOutputStream(file);
//                String result = new Gson().toJson(otaData, OtaData.class);
//                outputStream.write(result.getBytes());
//                outputStream.flush();
//                outputStream.close();
//
//                LogUtil.LOGOta("本地存储OtaData数据-updateOtaData", otaData.toString());
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//    }
//
//    /**
//     * 获取存储在本地的OtaData文件信息
//     * 如果存在文件，读取文件信息，如果不存在则new一个OtaData对象
//     *
//     * @return
//     */
//    public static OtaData getOtaData() {
//        OtaData otaData = null;
//        File file = new File(OtaConstant.FILE_FOLDER + "otadata.txt");
//        if (file.exists()) {
//            try {
//                FileInputStream inputStream = new FileInputStream(file);
//                int length = inputStream.available();
//                if (length > 0) {
//                    byte[] bb = new byte[length];
//                    inputStream.read(bb);
//                    String result = new String(bb);
//                    otaData = new OtaData(result);
//                }
//            } catch (FileNotFoundException e) {
//                e.printStackTrace();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        } else {
//            otaData = new OtaData();
//        }
//        LogUtil.LOGOta("本地存储OtaData数据-getOtaData", otaData.toString());
//        return otaData;
//    }

}

