package com.example.cisync.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {
    public static final String DB_NAME = "cisync.db";
    public static final int DB_VERSION = 1;

    public DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE users (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, email TEXT UNIQUE, password TEXT, role TEXT, has_org INTEGER, org_role TEXT)");

        db.execSQL("CREATE TABLE accountabilities (id INTEGER PRIMARY KEY AUTOINCREMENT, student_id INTEGER, description TEXT)");
        db.execSQL("CREATE TABLE documents (id INTEGER PRIMARY KEY AUTOINCREMENT, student_id INTEGER, status TEXT)");
        db.execSQL("CREATE TABLE notices (id INTEGER PRIMARY KEY AUTOINCREMENT, student_id INTEGER, content TEXT)");
        db.execSQL("CREATE TABLE transactions (id INTEGER PRIMARY KEY AUTOINCREMENT, student_id INTEGER, faculty_id INTEGER, message TEXT, timestamp TEXT)");
        db.execSQL("CREATE TABLE applications (id INTEGER PRIMARY KEY AUTOINCREMENT, student_id INTEGER, org_name TEXT, status TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS users");
        db.execSQL("DROP TABLE IF EXISTS accountabilities");
        db.execSQL("DROP TABLE IF EXISTS documents");
        db.execSQL("DROP TABLE IF EXISTS notices");
        db.execSQL("DROP TABLE IF EXISTS transactions");
        db.execSQL("DROP TABLE IF EXISTS applications");
        onCreate(db);
    }
}
