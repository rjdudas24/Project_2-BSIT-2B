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
        // Add verified field to users table
        db.execSQL("CREATE TABLE users (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, email TEXT UNIQUE, password TEXT, role TEXT, has_org INTEGER, org_role TEXT, verified INTEGER DEFAULT 0)");

        db.execSQL("CREATE TABLE accountabilities (id INTEGER PRIMARY KEY AUTOINCREMENT, student_id INTEGER, fee_name TEXT, amount TEXT, status INTEGER)");
        db.execSQL("CREATE TABLE documents (id INTEGER PRIMARY KEY AUTOINCREMENT, student_id INTEGER, status TEXT)");
        db.execSQL("CREATE TABLE notices (id INTEGER PRIMARY KEY AUTOINCREMENT, student_id INTEGER, content TEXT)");

        // Update transactions table to match what RegisterActivity uses
        db.execSQL("CREATE TABLE transactions (id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER, action_type TEXT, description TEXT, timestamp LONG)");

        db.execSQL("CREATE TABLE applications (id INTEGER PRIMARY KEY AUTOINCREMENT, student_id INTEGER, org_name TEXT, status TEXT)");

        // default admin account
        db.execSQL("INSERT INTO users (name, email, password, role, has_org, org_role, verified) VALUES " +
                "('Admin', 'admin@cisync.com', 'admin', 'Admin', 0, NULL, 1)");
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