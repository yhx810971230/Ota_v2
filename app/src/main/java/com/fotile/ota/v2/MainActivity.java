package com.fotile.ota.v2;

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

import com.fotile.ota.v2.bean.FileInfo;
import com.fotile.ota.v2.server.DownLoadServer;
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

        String url = "http://downloadz.dewmobile.net/Official/Kuaiya482.apk";
        String fileName = "Kuaiya482.apk";
        fileInfo = new FileInfo(fileName, url);

        //绑定服务
        Intent intent = new Intent(this, DownLoadServer.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.start:
                downLoadServer.start(fileInfo.url, this);
                break;
            case R.id.pause:
                downLoadServer.stop(fileInfo.url);
                break;
            case R.id.again:
                downLoadServer.restart(fileInfo.url, this);
                break;
            case R.id.clear:
                downLoadServer.clear(fileInfo.url);
                seekBar.setProgress(0);
                break;
        }
    }

    ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            DownLoadServer.DownLoadBinder downLoadBinder = (DownLoadServer.DownLoadBinder) service;
            downLoadServer = downLoadBinder.getServer();
            //初始化数据信息
            downLoadServer.initDownloadData(fileInfo);

            if (fileInfo.length > 0) {
                double f = OtaUtil.getProgress(fileInfo.finished, fileInfo.length);
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

//            OtaLog.LOGE("下载进度", f);
        }
    }

    @Override
    public void onDownError(FileInfo fileInfo, String error) {

    }

    @Override
    public void onDownCompleted(FileInfo fileInfo) {

    }

    @Override
    public void onDownStop(FileInfo fileInfo) {

    }

}
