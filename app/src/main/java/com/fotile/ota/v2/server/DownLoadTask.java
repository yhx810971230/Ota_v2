package com.fotile.ota.v2.server;

import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Message;

import com.fotile.ota.v2.OnProgressListener;
import com.fotile.ota.v2.bean.FileInfo;
import com.fotile.ota.v2.db.DbHelper;
import com.fotile.ota.v2.util.DownStatus;
import com.fotile.ota.v2.util.OtaLog;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;

import static com.fotile.ota.v2.util.OtaConstant.FILE_PATH;
import static com.fotile.ota.v2.util.OtaConstant.TEMP_FILE_NAME;

public class DownLoadTask extends Thread {
    private FileInfo info;
    private SQLiteDatabase db;
    private DbHelper helper;
    /**
     * 当前已下载完成的进度
     */
    private int finished = 0;
    private Object lock_status = new Object();

    private static final int MSG_DOWN_ERROR = 1001;
    private static final int MSG_DOWN_COMPLETED = 1002;
    private static final int MSG_DOWN_DOWNING = 1003;
    private static final int MSG_DOWN_STOP = 1004;

    private OnProgressListener listener;
    /**
     * 开始下载时的时间戳
     */
    private long time_start;

    public DownLoadTask(FileInfo info, DbHelper helper, OnProgressListener listener) {
        this.info = info;
        this.helper = helper;
        this.db = helper.getReadableDatabase();
        this.listener = listener;
        //新建了一个下载任务，不论该任务在数据库缓存中是否存在，下载状态均要设置为init
        setStatus(DownStatus.DOWN_STATUS_INIT);
    }

    @Override
    public void run() {
        getLength();
        time_start = System.currentTimeMillis();
        HttpURLConnection connection = null;
        RandomAccessFile rwd = null;
        try {
            URL url = new URL(info.url);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(3000);
            //从上次下载完成的地方下载
            int start = info.finished;
            //设置下载位置(从服务器上取要下载文件的某一段)
            connection.setRequestProperty("Range", "bytes=" + start + "-" + info.length);//设置下载范围
            //设置文件写入位置
            File file = new File(FILE_PATH, info.fileName + TEMP_FILE_NAME);
            rwd = new RandomAccessFile(file, "rwd");
            //从文件的某一位置开始写入
            rwd.seek(start);
            finished += info.finished;

            //在此处更新状态为下载中，因为在while循环中有stop的判断，不可更新为 MSG_DOWN_DOWNING
            setStatus(DownStatus.DOWN_STATUS_DOWNING);

            int code = connection.getResponseCode();
            if (code == 206 || code == 200) {//文件部分下载，返回码为206
                InputStream is = connection.getInputStream();
                byte[] buffer = new byte[1024 * 4];
                int len;
                while ((len = is.read(buffer)) != -1) {
                    //停止下载
                    if (info.status == DownStatus.DOWN_STATUS_STOP) {
                        //保存此次下载的进度
                        helper.updateData(db, info);
                        db.close();
                        //更新页面显示
                        Message msg = new Message();
                        msg.what = MSG_DOWN_STOP;
                        setStatus(DownStatus.DOWN_STATUS_STOP);
                        handler.sendMessage(msg);
                        return;
                    }

                    //写入文件
                    rwd.write(buffer, 0, len);
                    finished += len;
                    info.finished = finished;
                    //保存此次下载的进度
                    helper.updateData(db, info);

                    long time = System.currentTimeMillis();
                    if (time - time_start > 500) {
                        //更新界面显示
                        Message msg = new Message();
                        msg.what = MSG_DOWN_DOWNING;
                        //此处不更新状态，会将用户手动暂停的DOWN_STATUS_STOP状态重置
//                    setStatus( DownStatus.DOWN_STATUS_DOWNING, "MSG_DOWN_DOWNING");
                        handler.sendMessage(msg);
                        time_start = time;
                    }
                }

                //下载完成 保存此次下载的进度
                helper.updateData(db, info);
                db.close();
                //更新页面显示
                Message msg = new Message();
                msg.what = MSG_DOWN_COMPLETED;
                setStatus(DownStatus.DOWN_STATUS_COMPLETED);
                handler.sendMessage(msg);
            }
        } catch (Exception e) {
            e.printStackTrace();

            //更新页面显示
            Message msg = new Message();
            //错误信息上报
            msg.obj = e.getMessage();
            msg.what = MSG_DOWN_ERROR;
            setStatus(DownStatus.DOWN_STATUS_ERROR);
            handler.sendMessage(msg);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            try {
                if (rwd != null) {
                    rwd.close();
                }
            } catch (IOException e) {
                e.printStackTrace();

                //更新页面显示
                Message msg = new Message();
                //错误信息上报
                msg.obj = e.getMessage();
                msg.what = MSG_DOWN_ERROR;
                setStatus(DownStatus.DOWN_STATUS_ERROR);
                handler.sendMessage(msg);
            }
        }
    }

    /**
     * 首先开启一个线程去获取要下载文件的大小（长度）
     */
    private void getLength() {
        HttpURLConnection connection = null;
        try {
            //连接网络
            URL url = new URL(info.url);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(3000);
            int length = -1;
            if (connection.getResponseCode() == 200) {//网络连接成功
                //获得文件长度
                length = connection.getContentLength();
                OtaLog.LOGE("获取下载文件length", length);
            }
            if (length <= 0) {
                //连接失败
                return;
            }
            //创建文件保存路径
            File dir = new File(FILE_PATH);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            info.length = length;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            //释放资源
            try {
                if (connection != null) {
                    connection.disconnect();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 不同的线程访问这个变量，需要加锁
     */
    private void setStatus(DownStatus status) {
        synchronized (lock_status) {
            info.status = status;
        }
    }

    /**
     * 停止下载任务
     */
    public void stopTask() {
        setStatus(DownStatus.DOWN_STATUS_STOP);
    }

    public FileInfo getFileInfo() {
        return info;
    }

    /**
     * 更新进度
     */
    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                //下载完成
                case MSG_DOWN_COMPLETED:
                    if (null != listener) {
                        //下载完成rename文件
                        File file = new File(FILE_PATH, info.fileName + TEMP_FILE_NAME);
                        File newFile = new File(FILE_PATH, info.fileName);
                        file.renameTo(newFile);
                        //通知下载中，更新进度，因为进度条是1秒更新一次，防止未更新到100%
                        listener.onDownLoading(info);
                        listener.onDownCompleted(info);
                    }
                    break;
                //下载错误
                case MSG_DOWN_ERROR:
                    if (null != listener) {
                        listener.onDownError(info, msg.obj.toString());
                    }
                    break;
                //下载停止
                case MSG_DOWN_STOP:
                    if (null != listener) {
                        listener.onDownStop(info);
                    }
                    break;
                //下载中
                case MSG_DOWN_DOWNING:
                    if (null != listener) {
                        listener.onDownLoading(info);
                    }
                    break;
            }
            return false;
        }
    });
}
