package com.aledgroup.takepic.helper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.aledgroup.takepic.Common.CfUserInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by aled on 04/28/2016.
 */
public class SQLiteHandler extends SQLiteOpenHelper {

    private static final String TAG = SQLiteHandler.class.getSimpleName();

    // All Static variables
    // Database Version
    private static final int DATABASE_VERSION = 2;

    // Database Name
    private static final String DATABASE_NAME = "takepic_api.db";

    // Login table name
    private static final String TABLE_CfUser = "CfUser";
    private static final String TABLE_Settings = "CfSettings";

    // Login Table Columns names
    private static final String KEY_RowId = "RowId";
    private static final String KEY_loginCode= "LoginCode";
    private static final String KEY_UserName = "UserName";
    private static final String KEY_UserAlias = "UserAlias";
    private static final String KEY_TemplateUser = "TemplateUser";
    private static final String KEY_ImageName = "ImageName";

    private static final String KEY_Address = "IpAddress";

    public SQLiteHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Creating Tables
    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_LOGIN_TABLE = "CREATE TABLE " + TABLE_CfUser + "("
                + KEY_RowId + " VARCHAR(38) UNIQUE ," + KEY_loginCode + " VARCHAR(150),"
                + KEY_UserName + " VARCHAR(100)," + KEY_UserAlias + " VARCHAR(100),"
                + KEY_TemplateUser + " TEXT," + KEY_ImageName + " TEXT"+ ")";
        db.execSQL(CREATE_LOGIN_TABLE);

        String CREATE_SETTINGS_TABLE = "CREATE TABLE " + TABLE_Settings + "("
                + KEY_RowId + " VARCHAR(38) UNIQUE ," + KEY_Address + " TEXT"+ ")";
        db.execSQL(CREATE_SETTINGS_TABLE);

        Log.d(TAG, "Database tables created");
    }

    // Upgrading database
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CfUser);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_Settings);

        // Create tables again
        onCreate(db);
    }

    /**
     * Getting user data from database
     * */
    public HashMap<String, String> getUserDetails() {
        HashMap<String, String> user = new HashMap<String, String>();
        String selectQuery = "SELECT  * FROM " + TABLE_CfUser;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
        // Move to first row
        cursor.moveToFirst();
        if (cursor.getCount() > 0) {
            user.put("RowId", cursor.getString(1));
            user.put("loginCode", cursor.getString(2));
            user.put("UserName", cursor.getString(3));
            user.put("UserAlias", cursor.getString(4));
            user.put("TemplateUser", cursor.getString(5));
            user.put("ImageName", cursor.getString(6));
        }
        cursor.close();
        db.close();
        // return user
        Log.d(TAG, "Fetching user from Sqlite: " + user.toString());

        return user;
    }

    /**
     * Re crate database Delete all tables and create them again
     * */
    public void deleteUsers() {
        SQLiteDatabase db = this.getWritableDatabase();
        // Delete All Rows
        db.delete(TABLE_CfUser, null, null);
        db.close();
    }

    public void deleteSettings() {
        SQLiteDatabase db = this.getWritableDatabase();
        // Delete All Rows
        db.delete(TABLE_Settings, null, null);
        db.close();
    }

    /**
     * Storing user details in database
     * */
    public void addUserInfo(String rowId, String loginCode, String userName, String userAlias, String templateUser) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_RowId, rowId);
        values.put(KEY_loginCode, loginCode);
        values.put(KEY_UserName, userName);
        values.put(KEY_UserAlias, userAlias);
        values.put(KEY_TemplateUser, templateUser);

        // Inserting Row
        long id = db.insert(TABLE_CfUser, null, values);
        db.close(); // Closing database connection
    }

    // Getting All UserInfo
    public List<CfUserInfo> getAllUserInfo() {
        List<CfUserInfo> userInfoList = new ArrayList<>();
        // Select All Query
        String selectQuery = "SELECT  * FROM " + TABLE_CfUser;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                CfUserInfo userInfo = new CfUserInfo();
                userInfo.setRowId(cursor.getString(0));
                userInfo.setLoginCode(cursor.getString(1));
                userInfo.setUserName(cursor.getString(2));
                userInfo.setUserAlias(cursor.getString(3));
                userInfo.setTemplateUser(cursor.getString(4));
                // Adding contact to list
                userInfoList.add(userInfo);
            } while (cursor.moveToNext());
        }

        // return userInfo List
        return userInfoList;
    }

    // Updating single user
    public int updatUserInfo(CfUserInfo userInfo) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_TemplateUser, userInfo.getTemplateUser());

        // updating row
        return db.update(TABLE_CfUser, values, KEY_RowId + " = ?",
                new String[] { String.valueOf(userInfo.getRowId()) });
    }

    public void addSettings(String rowId, String ipAddress) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_RowId, rowId);
        values.put(KEY_Address, ipAddress);

        // Inserting Row
        long id = db.insert(TABLE_Settings, null, values);
        db.close(); // Closing database connection
    }

    public int updateSettings(CfUserInfo userInfo) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_Address, userInfo.getSettings());

        // updating row
        return db.update(TABLE_Settings, values, KEY_RowId + " = ?",
                new String[] { String.valueOf(userInfo.getRowId()) });
    }

    public List<CfUserInfo> getAllSettings() {
        List<CfUserInfo> userInfoList = new ArrayList<>();
        // Select All Query
        String selectQuery = "SELECT  * FROM " + TABLE_Settings;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                CfUserInfo userInfo = new CfUserInfo();
                userInfo.setRowId(cursor.getString(0));
                userInfo.setSettings(cursor.getString(1));
                // Adding contact to list
                userInfoList.add(userInfo);
            } while (cursor.moveToNext());
        }

        // return userInfo List
        return userInfoList;
    }
}
