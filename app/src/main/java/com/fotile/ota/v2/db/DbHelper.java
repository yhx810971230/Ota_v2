package com.fotile.ota.v2.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.fotile.ota.v2.bean.FileInfo;
import com.fotile.ota.v2.util.OtaUtil;

public class DbHelper extends SQLiteOpenHelper {

    //表名
    public static String TABLE = "file";
    public static final int VERSION = 1;
    private Object lock_update = new Object();

    final String FILE_NAME = "file_name";
    /**
     * url前缀（?前面的字符串）
     */
    final String URL_PRE = "url_pre";
    /**
     * url后缀（?后面的字符串）
     */
    final String URL_SUF ="url_suf";
    final String LENGTH = "length";
    final String FINISHED = "finished";

    public DbHelper(Context context) {
        super(context, "download.db", null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        //文件名，下载地址，下载文件的总长度，当前下载完成长度
        db.execSQL("create table file("
                + FILE_NAME + " varchar,"
                + URL_PRE + " varchar,"
                + URL_SUF + " varchar,"
                + LENGTH + " integer,"
                + FINISHED + " integer)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    /**
     * 插入一条下载信息
     */
    public void insertData(SQLiteDatabase db, FileInfo info) {
        //如果不存在该条信息，执行插入数据
        if (!isExist(db, info.getUrl())) {
            ContentValues values = new ContentValues();
            values.put(FILE_NAME, info.file_name);
            values.put(URL_PRE, info.url_pre);
            values.put(URL_SUF, info.url_suf);
            values.put(LENGTH, info.length);
            values.put(FINISHED, info.finished);
            db.insert(TABLE, null, values);
        }
        //如果存在该条信息，更新一下url_suf，同一个文件在链接有效期过后会有不同的后缀
        else {
            ContentValues values = new ContentValues();
            values.put(URL_SUF, info.url_suf);
            db.update(TABLE, values, URL_PRE + " = ?", new String[]{ info.url_pre });
        }
    }

    /**
     * 是否已经插入这条数据
     */
    private boolean isExist(SQLiteDatabase db, String url) {
        String url_pre = OtaUtil.getUrlPre(url);
        Cursor cursor = db.query(TABLE, null, URL_PRE + " = ?", new String[]{ url_pre }, null, null, null, null);
        boolean exist = cursor.moveToNext();
        cursor.close();
        return exist;
    }

    /**
     * 更新数据库中的数据，如果不存在则添加一条数据
     *
     * @param db
     * @param fileInfo
     */
    public void updateData(SQLiteDatabase db, FileInfo fileInfo) {
        synchronized (lock_update) {
            ContentValues values = new ContentValues();
            values.put(FINISHED, fileInfo.finished);
            values.put(LENGTH, fileInfo.length);
            db.update(TABLE, values, URL_PRE + " = ?", new String[]{ fileInfo.url_pre });
        }
    }

    /**
     * 查询已经存在的一条信息
     */
    public FileInfo queryData(SQLiteDatabase db, String url) {
        String url_pre = OtaUtil.getUrlPre(url);
        Cursor cursor = db.query(TABLE, null, URL_PRE + " = ?", new String[]{ url_pre }, null, null, null, null);
        FileInfo info = null;
        if (cursor != null && cursor.getCount() > 0) {
            info = new FileInfo();
            while (cursor.moveToNext()) {
                String file_name = cursor.getString(cursor.getColumnIndex(FILE_NAME));
                String url_suf = cursor.getString(cursor.getColumnIndex(URL_SUF));
                int length = cursor.getInt(cursor.getColumnIndex(LENGTH));
                int finished = cursor.getInt(cursor.getColumnIndex(FINISHED));
                info.file_name = file_name;
                info.url_pre = url_pre;
                info.url_suf = url_suf;
                info.length = length;
                info.finished = finished;
            }
            cursor.close();
        }
        return info;
    }

    /**
     * 重置一条下载信息
     */
    public void resetData(SQLiteDatabase db, String url) {
        String url_pre = OtaUtil.getUrlPre(url);
        if (isExist(db, url)) {
            ContentValues values = new ContentValues();
            values.put(FINISHED, 0);
            values.put(LENGTH, 0);
            db.update(TABLE, values, URL_PRE + " = ?", new String[]{ url_pre });
        }
    }

    public void deleteData(SQLiteDatabase db, String url) {
        String url_pre = OtaUtil.getUrlPre(url);
        db.delete(TABLE, URL_PRE + " = ?", new String[]{ url_pre });
    }

}
