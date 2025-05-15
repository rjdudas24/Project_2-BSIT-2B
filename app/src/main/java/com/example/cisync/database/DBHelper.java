package com.example.cisync.database;

import android.content.Context;
import android.database.Cursor;
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
        // Users table with verified field
        db.execSQL("CREATE TABLE users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT, " +
                "email TEXT UNIQUE, " +
                "password TEXT, " +
                "role TEXT, " +
                "has_org INTEGER DEFAULT 0, " +
                "org_role TEXT, " +
                "verified INTEGER DEFAULT 0)");

        // Accountabilities table
        db.execSQL("CREATE TABLE accountabilities (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "student_id INTEGER, " +
                "fee_name TEXT, " +
                "amount TEXT, " +
                "status INTEGER)");

        // Documents table
        db.execSQL("CREATE TABLE documents (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "student_id INTEGER, " +
                "status TEXT)");

        // Notices table
        db.execSQL("CREATE TABLE notices (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "student_id INTEGER, " +
                "content TEXT)");

        // Transactions table
        db.execSQL("CREATE TABLE transactions (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id INTEGER, " +
                "action_type TEXT, " +
                "description TEXT, " +
                "timestamp LONG)");

        // Applications table - for the approval system
        db.execSQL("CREATE TABLE applications (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "student_id INTEGER, " +
                "org_name TEXT, " +
                "status TEXT, " +
                "department TEXT, " +
                "created_at LONG DEFAULT (strftime('%s','now') * 1000))");

        // Default admin account
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

    public void addSampleAccountabilities(int studentId) {
        SQLiteDatabase db = this.getWritableDatabase();

        // Clear any existing accountabilities for this student
        db.delete("accountabilities", "student_id = ?", new String[]{String.valueOf(studentId)});

        // Add sample data
        String[] feeNames = {"College Fee", "Fines"};
        String[] amounts = {"500", "210"};
        int[] statuses = {1, 0,};  // 1 = paid, 0 = unpaid

        for (int i = 0; i < feeNames.length; i++) {
            db.execSQL(
                    "INSERT INTO accountabilities (student_id, fee_name, amount, status) VALUES (?, ?, ?, ?)",
                    new Object[]{studentId, feeNames[i], amounts[i], statuses[i]}
            );
        }

        db.close();
    }
}