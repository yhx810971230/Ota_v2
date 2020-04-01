package com.fotile.ota.v2.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.fotile.ota.v2.R;
import com.fotile.ota.v2.bean.UpgradeInfo;
import com.fotile.ota.v2.server.DownLoadServer;
import com.fotile.ota.v2.util.DownStatus;
import com.fotile.ota.v2.util.OtaLog;
import com.fotile.ota.v2.util.OtaUpgradeUtil;
import com.google.gson.Gson;

import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CheckActivity extends AppCompatActivity {

    private TextView txtCheck;
    private UpgradeInfo upgradeInfo;
    //请求中
    private static final int REQUEST_CHECKING = 1000;
    //获取超时
    private static final int REQUEST_TIMEOUT = 1001;
    //服务器无新的升级包
    public static final int REQUEST_NO_PACKAGE = 1002;
    //获取数据异常
    public static final int REQUEST_ERROR = 1003;
    //有新的升级包-未下载或者未下载完整
    public static final int REQUEST_EXIT_UNDOWNLOAD = 1004;
    //有新的升级包-已下载完整
    public static final int REQUEST_EXIT_DOWNLOADED = 1005;
    //有新的升级包-下载中
    public static final int REQUEST_EXIT_DOWNLOADING = 1006;

    public static final int JUMP_DELAY = 1008;
    /**
     * 下载状态
     */
    private DownStatus download_state = DownStatus.DOWN_STATUS_INIT;
    DownLoadServer downLoadServer;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check);
        bindServer();
        txtCheck = (TextView) findViewById(R.id.txt_check);
        txtCheck.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        getOtaData();
                    }
                }).start();
            }
        });
    }


    private void bindServer() {
        //绑定服务
        Intent intent = new Intent(this, DownLoadServer.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            DownLoadServer.DownLoadBinder downLoadBinder = (DownLoadServer.DownLoadBinder) service;
            downLoadServer = downLoadBinder.getServer();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    /**
     * 获取OTA服务器上的固件的信息
     */
    private void getOtaData() {
        String packageName = "com.fotile.test";
        String currentVersion = "123413";
        String token = "QWjpHRpVjSXV3aPJXQxdWZ54mULlncwR1b3l0bahGV1dGd2pXcBZzVxMDNGJUNFJDMwADMwADMwADM==";
        String XUser = "777778225";
        String XSource = "android.4.3.5";

        String reqUrl = OtaUpgradeUtil.buildUrl(packageName, currentVersion);
//        //测试使用
//        if (OtaConstant.OTA_URL_TEST) {
//            reqUrl = OtaConstant.test_url;
//        }
        //请求头信息
        Map<String, String> heads = new HashMap<>();
        heads.put("Content-Type", "application/json");
        heads.put("Access-Token", token);
        heads.put("X-User", XUser);
        heads.put("X-Source", XSource);

        OtaLog.LOGE("请求Ota包信息url", reqUrl);
        String content = "";
        String miwen = "";
        String message = "";
        try {
            content = OtaUpgradeUtil.httpGet(reqUrl, heads);
            //没有可更新的包
            if (content == null || content.equals("{}")) {
                checkhandler.sendEmptyMessage(REQUEST_NO_PACKAGE);
                return;
            }
            JSONObject jo = new JSONObject(content);
            message = jo.getString("message");
//            mingwen = otaUpgradeUtil.Decrypt(miwen, OtaConstant.PASSWORD);
            OtaLog.LOGE("请求Ota包信息返回数据", message);
        }
        //请求超时
        catch (IOException e) {
            e.printStackTrace();
            checkhandler.sendEmptyMessage(REQUEST_TIMEOUT);
            return;
        }
        //请求错误
        catch (Exception e) {
            e.printStackTrace();
            checkhandler.sendEmptyMessage(REQUEST_ERROR);
            return;
        }
        Gson parser = new Gson();
        upgradeInfo = parser.fromJson(message, UpgradeInfo.class);

        //根据url获取当前的下载状态
        download_state = downLoadServer.getCurrentStatus(upgradeInfo.url);
        //下载中
        if (download_state == DownStatus.DOWN_STATUS_DOWNING) {
            checkhandler.sendEmptyMessage(REQUEST_EXIT_DOWNLOADING);
        } else {
            //下载包在本地已经存在
            if (OtaUpgradeUtil.exitOtaFile(upgradeInfo)) {
                checkhandler.sendEmptyMessage(REQUEST_EXIT_DOWNLOADED);
            }
            //下载包在本地不存在或者未下载完整
            else {
                checkhandler.sendEmptyMessage(REQUEST_EXIT_UNDOWNLOAD);
            }
        }

    }


    /**
     * 固件包升级信息回调
     * 进入该回调，mInfo有值了
     */
    private Handler checkhandler = new Handler(Looper.getMainLooper()) {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                //超时
                case REQUEST_TIMEOUT:
                    txtCheck.setText("请求超时");
                    break;
                //无下载包
                case REQUEST_NO_PACKAGE:
                    txtCheck.setText("无下载包");
                    break;
                //存在下载url-未下载
                case REQUEST_EXIT_UNDOWNLOAD:
                    txtCheck.setText("存在下载url-未下载");
                    checkhandler.sendEmptyMessageDelayed(JUMP_DELAY, 2000);
                    break;
                //存在下载url-已下载
                case REQUEST_EXIT_DOWNLOADED:
                    txtCheck.setText("存在下载url-已下载");
                    checkhandler.sendEmptyMessageDelayed(JUMP_DELAY, 2000);
                    break;
                //存在下载url-下载中
                case REQUEST_EXIT_DOWNLOADING:
                    txtCheck.setText("存在下载url-下载中");
                    checkhandler.sendEmptyMessageDelayed(JUMP_DELAY, 2000);
                    break;
                //请求错误
                case REQUEST_ERROR:
                    txtCheck.setText("请求错误");
                    break;
                case JUMP_DELAY:
                    Intent intent = new Intent(CheckActivity.this, MainActivity.class);
                    intent.putExtra("url", upgradeInfo.url);
                    startActivity(intent);
                    break;
            }
        }
    };

}
