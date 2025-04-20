package com.example.cisync.activities;

import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.*;
import com.example.cisync.R;
import com.example.cisync.database.DBHelper;
import java.text.SimpleDateFormat;
import java.util.*;

public class FacultyHistoryActivity extends Activity {

    ListView lvHistory;
    ArrayList<String> historyList = new ArrayList<>();
    ArrayList<Integer> transactionIds = new ArrayList<>();
    ArrayAdapter<String> adapter;
    DBHelper dbHelper;
    int facultyId = 1; // demo static ID

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faculty_history);

        lvHistory = findViewById(R.id.lvHistory);
        dbHelper = new DBHelper(this);
        loadHistory();

        lvHistory.setOnItemClickListener((adapterView, view, i, l) -> {
            int originalId = transactionIds.get(i);
            resendTransaction(originalId);
        });
    }

    private void loadHistory() {
        historyList.clear();
        transactionIds.clear();

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id, message, timestamp FROM transactions WHERE faculty_id=?", new String[]{String.valueOf(facultyId)});

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(0);
                String msg = cursor.getString(1);
                String time = cursor.getString(2);
                historyList.add("[" + time + "] " + msg);
                transactionIds.add(id);
            } while (cursor.moveToNext());
        }

        cursor.close();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, historyList);
        lvHistory.setAdapter(adapter);
    }

    private void resendTransaction(int originalId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT student_id, message FROM transactions WHERE id=?", new String[]{String.valueOf(originalId)});

        if (cursor.moveToFirst()) {
            int studentId = cursor.getInt(0);
            String originalMessage = cursor.getString(1);

            ContentValues cv = new ContentValues();
            cv.put("student_id", studentId);
            cv.put("faculty_id", facultyId);
            cv.put("message", "[RESEND] " + originalMessage);
            cv.put("timestamp", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
            db.insert("transactions", null, cv);

            Toast.makeText(this, "Notification resent.", Toast.LENGTH_SHORT).show();
            loadHistory();
        }

        cursor.close();
    }
}
