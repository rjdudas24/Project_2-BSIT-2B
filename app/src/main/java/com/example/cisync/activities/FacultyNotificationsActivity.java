package com.example.cisync.activities;

import android.app.Activity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import com.example.cisync.R;
import com.example.cisync.database.DBHelper;
import java.util.ArrayList;

public class FacultyNotificationsActivity extends Activity {

    ListView lvFacultyNotifications;
    ArrayAdapter<String> adapter;
    ArrayList<String> notifications = new ArrayList<>();
    DBHelper dbHelper;
    ImageView btnBackNotifications;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faculty_notifications);

        lvFacultyNotifications = findViewById(R.id.lvFacultyNotifications);
        btnBackNotifications = findViewById(R.id.btnBackNotifications);
        dbHelper = new DBHelper(this);

        loadNotifications();

        btnBackNotifications.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Close this activity and return to previous
            }
        });
    }

    private void loadNotifications() {
        notifications.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT description, timestamp FROM transactions WHERE user_id = ?", new String[]{"1"}); // static for demo

        if (cursor.moveToFirst()) {
            do {
                String description = cursor.getString(0);
                long timestamp = cursor.getLong(1);
                String formatted = description + "\n(" + android.text.format.DateFormat.format("yyyy-MM-dd hh:mm:ss a", timestamp) + ")";
                notifications.add(formatted);
            } while (cursor.moveToNext());
        }

        cursor.close();

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, notifications);
        lvFacultyNotifications.setAdapter(adapter);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed(); // Ensure system back button works too
    }
}
