package com.example.cisync.activities;

import android.app.Activity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.*;
import com.example.cisync.R;
import com.example.cisync.database.DBHelper;

import java.util.ArrayList;

public class ViewAccountabilitiesActivity extends Activity {

    ListView rvAccountabilities;
    DBHelper dbHelper;
    ArrayList<String> accountList = new ArrayList<>();
    ArrayAdapter<String> adapter;
    int studentId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_accountabilities);

        rvAccountabilities = findViewById(R.id.rvAccountabilities);
        dbHelper = new DBHelper(this);

        // Get student ID from intent
        studentId = getIntent().getIntExtra("studentId", -1);

        loadAccountabilities();
    }

    private void loadAccountabilities() {
        accountList.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Use the actual student ID passed from intent
        if (studentId != -1) {
            Cursor cursor = db.rawQuery("SELECT description FROM accountabilities WHERE student_id=?",
                    new String[]{String.valueOf(studentId)});

            if (cursor.moveToFirst()) {
                do {
                    accountList.add(cursor.getString(0));
                } while (cursor.moveToNext());
            } else {
                // No accountabilities found
                accountList.add("No accountabilities found");
            }

            cursor.close();
        } else {
            // Invalid student ID
            accountList.add("Error: Invalid user ID");
        }

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, accountList);
        rvAccountabilities.setAdapter(adapter);
    }
}