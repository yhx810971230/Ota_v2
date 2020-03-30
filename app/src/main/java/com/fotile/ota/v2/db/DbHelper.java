package com.fotile.ota.v2.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.fotile.ota.v2.bean.FileInfo;

public class DbHelper extends SQLiteOpenHelper {

    //表名
    public static String TABLE = "file";
    public static final int VERSION = 1;
    private Object lock_update = new Object();

    public DbHelper(Context context) {
        super(context, "download.db", null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        //文件名，下载地址，下载文件的总长度，当前下载完成长度
        db.execSQL("create table file(fileName varchar,url varchar,length integer,finished integer)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    /**
     * 插入一条下载信息
     */
    public void insertData(SQLiteDatabase db, FileInfo info) {
        if (!isExist(db, info.url)) {
            ContentValues values = new ContentValues();
            values.put("fileName", info.fileName);
            values.put("url", info.url);
            values.put("length", info.length);
            values.put("finished", info.finished);
            db.insert(TABLE, null, values);
        }
    }

    /**
     * 是否已经插入这条数据
     */
    private boolean isExist(SQLiteDatabase db, String url) {
        Cursor cursor = db.query(TABLE, null, "url = ?", new String[]{url}, null, null, null, null);
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
            values.put("finished", fileInfo.finished);
            values.put("length", fileInfo.length);
            db.update(TABLE, values, "url = ?", new String[]{fileInfo.url});
        }
    }

    /**
     * 查询已经存在的一条信息
     */
    public FileInfo queryData(SQLiteDatabase db, String url) {
        Cursor cursor = db.query(TABLE, null, "url = ?", new String[]{url}, null, null, null, null);
        FileInfo info = null;
        if (cursor != null && cursor.getCount() > 0) {
            info = new FileInfo();
            while (cursor.moveToNext()) {
                String fileName = cursor.getString(cursor.getColumnIndex("fileName"));
                int length = cursor.getInt(cursor.getColumnIndex("length"));
                int finished = cursor.getInt(cursor.getColumnIndex("finished"));
                info.fileName = fileName;
                info.url = url;
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
        if (isExist(db, url)) {
            ContentValues values = new ContentValues();
            values.put("finished", 0);
            values.put("length", 0);
            db.update(TABLE, values, "url = ?", new String[]{url});
        }
    }

    public void deleteData(SQLiteDatabase db, String url) {
        db.delete(TABLE, "url = ?", new String[]{url});
    }

}
