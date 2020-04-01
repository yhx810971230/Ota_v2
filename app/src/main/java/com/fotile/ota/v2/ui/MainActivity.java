package com.fotile.ota.v2.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;

import com.fotile.ota.v2.OnProgressListener;
import com.fotile.ota.v2.R;
import com.fotile.ota.v2.bean.FileInfo;
import com.fotile.ota.v2.server.DownLoadServer;
import com.fotile.ota.v2.util.OtaLog;
import com.fotile.ota.v2.util.OtaUtil;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, OnProgressListener {

    private SeekBar seekBar;
    private Button btnStart;
    private Button btnPause;
    private Button btnAgain;
    private Button btnClear;

    FileInfo fileInfo;
    DownLoadServer downLoadServer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        seekBar = (SeekBar) findViewById(R.id.seekbar);
        btnStart = (Button) findViewById(R.id.start);
        btnPause = (Button) findViewById(R.id.pause);
        btnAgain = (Button) findViewById(R.id.again);
        btnClear = (Button) findViewById(R.id.clear);

        btnStart.setOnClickListener(this);
        btnPause.setOnClickListener(this);
        btnAgain.setOnClickListener(this);
        btnClear.setOnClickListener(this);

        String url = getIntent().getStringExtra("url");
        String fileName = OtaUtil.getFileName(url);
        fileInfo = new FileInfo(fileName, url);

        //绑定服务
        Intent intent = new Intent(this, DownLoadServer.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.start:
                //在开始下载前，需要保证数据库中有一条下载信息
                downLoadServer.addTask(fileInfo);
                downLoadServer.start(fileInfo.getUrl(), this);
                break;
            case R.id.pause:
                downLoadServer.stop(fileInfo.getUrl());
                break;
            case R.id.again:
                downLoadServer.restart(fileInfo.getUrl(), this);
                break;
            case R.id.clear:
                downLoadServer.clear(fileInfo.getUrl());
                seekBar.setProgress(0);
                break;
        }
    }

    ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            DownLoadServer.DownLoadBinder downLoadBinder = (DownLoadServer.DownLoadBinder) service;
            downLoadServer = downLoadBinder.getServer();

            //获取已经下载的信息，更新进度条
            FileInfo cacheInfo = downLoadServer.getDownCacheInfo(fileInfo.getUrl());
            if (null != cacheInfo && cacheInfo.length > 0) {
                double f = OtaUtil.getProgress(cacheInfo.finished, cacheInfo.length);
                f = f < 0 ? 0 : f;
                f = f > 1 ? 1 : f;
                int seek_progress = (int) (f * 100);
                seekBar.setProgress(seek_progress);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    @Override
    public void onDownLoading(FileInfo fileInfo) {
        if (fileInfo.length > 0) {
            double f = OtaUtil.getProgress(fileInfo.finished, fileInfo.length);
            f = f < 0 ? 0 : f;
            f = f > 1 ? 1 : f;
            int seek_progress = (int) (f * 100);

            seekBar.setProgress(seek_progress);

            OtaLog.LOGE("下载进度", f);
        }
    }

    @Override
    public void onDownError(FileInfo fileInfo, String error) {
        OtaLog.LOGE("文件下载错误onDownError", error);
    }

    @Override
    public void onDownCompleted(FileInfo fileInfo) {

    }

    @Override
    public void onDownStop(FileInfo fileInfo) {

    }

}
