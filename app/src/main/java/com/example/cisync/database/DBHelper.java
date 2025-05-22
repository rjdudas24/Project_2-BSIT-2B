package com.example.cisync.database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DBHelper extends SQLiteOpenHelper {
    public static final String DB_NAME = "cisync.db";
    public static final int DB_VERSION = 1;
    private static final String TAG = "DBHelper";

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
                "name TEXT, " +
                "description TEXT, " +
                "status TEXT, " +
                "created_by TEXT, " +
                "timestamp TEXT)");

        // Notices table
        db.execSQL("CREATE TABLE notices (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "student_id INTEGER, " +
                "content TEXT, " +
                "timestamp TEXT, " +
                "title TEXT, " +
                "target_type TEXT DEFAULT 'ALL', " +
                "target_student_id INTEGER DEFAULT NULL, " +
                "posted_by_name TEXT, " +
                "posted_by_position TEXT)");

        // Transactions table
        db.execSQL("CREATE TABLE transactions (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id INTEGER, " +
                "action_type TEXT, " +
                "description TEXT, " +
                "timestamp LONG, " +
                "read_status INTEGER DEFAULT 0)");

        // Applications table - for the approval system
        db.execSQL("CREATE TABLE applications (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "student_id INTEGER, " +
                "org_name TEXT, " +
                "status TEXT, " +
                "department TEXT, " +
                "created_at LONG DEFAULT (strftime('%s','now') * 1000))");

        // Faculty Inquiries table
        db.execSQL("CREATE TABLE faculty_inquiries (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "student_id INTEGER NOT NULL, " +
                "faculty_name TEXT NOT NULL, " +
                "department TEXT NOT NULL, " +
                "subject TEXT NOT NULL, " +
                "description TEXT NOT NULL, " +
                "status TEXT NOT NULL, " +
                "created_at TEXT NOT NULL, " +
                "FOREIGN KEY (student_id) REFERENCES users(id))");

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

    public void enhanceNoticesTableIfNeeded() {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            // Check if notices table exists
            Cursor cursor = db.rawQuery(
                    "SELECT name FROM sqlite_master WHERE type='table' AND name='notices'",
                    null
            );

            if (cursor.getCount() > 0) {
                cursor.close();

                // Check existing columns - FIXED getColumnIndex issue
                cursor = db.rawQuery("PRAGMA table_info(notices)", null);
                boolean hasTitle = false;
                boolean hasTargetType = false;
                boolean hasPostedBy = false;

                if (cursor.moveToFirst()) {
                    do {
                        int nameIndex = cursor.getColumnIndex("name");
                        if (nameIndex >= 0) { // Check if column exists before using index
                            String columnName = cursor.getString(nameIndex);
                            if ("title".equals(columnName)) hasTitle = true;
                            if ("target_type".equals(columnName)) hasTargetType = true;
                            if ("posted_by_name".equals(columnName)) hasPostedBy = true;
                        }
                    } while (cursor.moveToNext());
                }
                cursor.close();

                // Add missing columns only if they don't exist
                if (!hasTitle) {
                    db.execSQL("ALTER TABLE notices ADD COLUMN title TEXT");
                    Log.d(TAG, "Added title column to notices table");
                }
                if (!hasTargetType) {
                    db.execSQL("ALTER TABLE notices ADD COLUMN target_type TEXT DEFAULT 'ALL'");
                    db.execSQL("ALTER TABLE notices ADD COLUMN target_student_id INTEGER DEFAULT NULL");
                    Log.d(TAG, "Added targeting columns to notices table");
                }
                if (!hasPostedBy) {
                    db.execSQL("ALTER TABLE notices ADD COLUMN posted_by_name TEXT");
                    db.execSQL("ALTER TABLE notices ADD COLUMN posted_by_position TEXT");
                    Log.d(TAG, "Added posted_by columns to notices table");
                }

            } else {
                // Table doesn't exist, create it with enhanced structure
                cursor.close();
                db.execSQL("CREATE TABLE notices (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "student_id INTEGER, " +
                        "content TEXT, " +
                        "timestamp TEXT, " +
                        "title TEXT, " +
                        "target_type TEXT DEFAULT 'ALL', " +
                        "target_student_id INTEGER DEFAULT NULL, " +
                        "posted_by_name TEXT, " +
                        "posted_by_position TEXT)");
                Log.d(TAG, "Created enhanced notices table");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error enhancing notices table: " + e.getMessage(), e);
        } finally {
            db.close();
        }
    }

    public void addSampleAccountabilities(int studentId) {
        SQLiteDatabase db = this.getWritableDatabase();

        try {
            // Clear any existing accountabilities for this student
            db.delete("accountabilities", "student_id = ?", new String[]{String.valueOf(studentId)});

            // Add sample data
            String[] feeNames = {"College Fee", "Fines"};
            String[] amounts = {"500", "210"};
            int[] statuses = {1, 0};  // 1 = paid, 0 = unpaid

            for (int i = 0; i < feeNames.length; i++) {
                db.execSQL(
                        "INSERT INTO accountabilities (student_id, fee_name, amount, status) VALUES (?, ?, ?, ?)",
                        new Object[]{studentId, feeNames[i], amounts[i], statuses[i]}
                );
            }
        } catch (Exception e) {
            Log.e(TAG, "Error adding sample accountabilities: " + e.getMessage(), e);
        } finally {
            db.close();
        }
    }
}