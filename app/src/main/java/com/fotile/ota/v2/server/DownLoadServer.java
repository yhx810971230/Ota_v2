package com.fotile.ota.v2.server;

import android.app.Service;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Binder;
import android.os.IBinder;

import com.fotile.ota.v2.OnProgressListener;
import com.fotile.ota.v2.bean.FileInfo;
import com.fotile.ota.v2.db.DbHelper;
import com.fotile.ota.v2.util.DownStatus;
import com.fotile.ota.v2.util.OtaLog;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static com.fotile.ota.v2.util.OtaConstant.FILE_PATH;
import static com.fotile.ota.v2.util.OtaConstant.TEMP_FILE_NAME;

/**
 * 文件名称：DownLoadServer
 * 创建时间：2020/3/25 11:30
 * 文件作者：yaohx
 * 功能描述：文件下载服务类，Client需要和该服务执行bind操作
 */
public class DownLoadServer extends Service {
    private DbHelper helper;
    private SQLiteDatabase db;
    /**
     * 一个url（完整的下载链接）对应一个下载线程
     * 一个下载线程的生命周期从开始下载到结束下载终止
     */
    private Map<String, DownLoadTask> mapTask = new HashMap<>();

    private Object lock_start = new Object();

    @Override
    public void onCreate() {
        super.onCreate();
        helper = new DbHelper(this);
        db = helper.getReadableDatabase();
        //创建文件夹目录
        File folder = new File(FILE_PATH);
        folder.mkdirs();
    }

    /**
     * 获取数据库中的缓存信息
     *
     * @param url 完整的下载url
     */
    public FileInfo getDownCacheInfo(String url) {
        db = helper.getReadableDatabase();
        //查询数据库中的缓存的下载文件信息
        FileInfo fileInfoDb = helper.queryData(db, url);
        return fileInfoDb;
    }

    /**
     * 将一条下载信息添加到数据库,如果数据库中有该条数据，则不执行操作
     *
     * @param fileInfo
     */
    public void addTask(FileInfo fileInfo) {
        db = helper.getWritableDatabase();
        //将下载信息更新到数据库
        helper.insertData(db, fileInfo);
    }

    /**
     * 开始下载任务，从fileinfo对象中标识的文件起始位开始下载
     * 开始下载时，应保证数据库中已经存在url对应的下载数据
     *
     * @param url 完整的下载url
     * @param onProgressListener
     * @return
     */
    public boolean start(String url, OnProgressListener onProgressListener) {
        synchronized (lock_start) {
            try {
                Thread.sleep(100);

                db = helper.getReadableDatabase();
                //去数据库中获取缓存的下载信息，包括下载进度，然后根据该下载进度去执行文件断点下载
                FileInfo fileInfo = helper.queryData(db, url);
                if (null == fileInfo) {
                    OtaLog.LOGE("下载信息FileInfo为空...", fileInfo);
                    return false;
                }

                //判断是否有相同的url下载任务正在执行
                DownLoadTask downLoadTask = mapTask.get(url);
                if (null != downLoadTask) {
                    if (downLoadTask.getFileInfo().status == DownStatus.DOWN_STATUS_DOWNING) {
                        onProgressListener.onDownError(fileInfo, "文件正在下载中...");
                        OtaLog.LOGE("文件正在下载中...", fileInfo);
                        return false;
                    }
                    //下载完成
                    if (downLoadTask.getFileInfo().status == DownStatus.DOWN_STATUS_COMPLETED) {
                        onProgressListener.onDownError(fileInfo, "文件已经下载完成...");
                        OtaLog.LOGE("文件已经下载完成...", fileInfo);
                        return false;
                    }
                }

                OtaLog.LOGE("下载开始FileInfo", fileInfo);
                //开始任务下载
                downLoadTask = new DownLoadTask(fileInfo, helper, onProgressListener);
                downLoadTask.start();
                mapTask.put(url, downLoadTask);
                return true;
            } catch (InterruptedException e) {
                e.printStackTrace();
                return false;
            }
        }
    }

    /**
     * 停止下载任务
     * @param url 完整的下载url
     */
    public void stop(String url) {
        DownLoadTask downLoadTask = mapTask.get(url);
        if (null != downLoadTask) {
            downLoadTask.stopTask();
        }
    }

    /**
     * 重新下载任务，将本地数据库缓存和sd卡文件删除后重新下载文件
     *
     * @param url
     */
    public void restart(String url, OnProgressListener onProgressListener) {
        try {
            stop(url);
            //sleep保证下载线程被暂停
            Thread.sleep(100);

            //删除本地下载文件
            DownLoadTask downLoadTask = mapTask.get(url);
            if (null != downLoadTask) {
                FileInfo fileInfo = downLoadTask.getFileInfo();
                deleFile(fileInfo);
            }

            //移除task
            mapTask.remove(url);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        db = helper.getWritableDatabase();
        //重置一条下载信息
        helper.resetData(db, url);
        start(url, onProgressListener);
    }

    public void clear(String url) {
        try {
            stop(url);
            //sleep保证下载线程被暂停
            Thread.sleep(100);

            //删除本地下载文件
            DownLoadTask downLoadTask = mapTask.get(url);
            if (null != downLoadTask) {
                FileInfo fileInfo = downLoadTask.getFileInfo();
                deleFile(fileInfo);
            }

            //移除task
            mapTask.remove(url);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        db = helper.getWritableDatabase();
        //删除数据库下载信息
        helper.deleteData(db, url);

    }

    private void deleFile(FileInfo fileInfo) {
        File file = new File(FILE_PATH, fileInfo.file_name);
        if (file.exists()) {
            file.delete();
        }
        file = new File(FILE_PATH, fileInfo.file_name + TEMP_FILE_NAME);
        if (file.exists()) {
            file.delete();
        }
    }

    /**
     * 获取当前任务状态
     */
    public DownStatus getCurrentStatus(String url) {
        DownLoadTask downLoadTask = mapTask.get(url);
        if (null != downLoadTask) {
            return downLoadTask.getFileInfo().status;
        }
        return DownStatus.DOWN_STATUS_INIT;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new DownLoadBinder();
    }

    public class DownLoadBinder extends Binder {
        public DownLoadServer getServer() {
            return DownLoadServer.this;
        }
    }
}
