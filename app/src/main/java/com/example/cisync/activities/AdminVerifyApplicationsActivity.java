package com.example.cisync.activities;

import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.*;
import com.example.cisync.R;
import com.example.cisync.database.DBHelper;

import java.util.ArrayList;

public class AdminVerifyApplicationsActivity extends Activity {

    ListView lvApplications;
    DBHelper dbHelper;
    ArrayList<String> appList = new ArrayList<>();
    ArrayList<Integer> appIds = new ArrayList<>();
    ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_verify_applications);

        lvApplications = findViewById(R.id.lvApplications);
        dbHelper = new DBHelper(this);

        loadApplications();

        lvApplications.setOnItemClickListener((adapterView, view, i, l) -> {
            int id = appIds.get(i);
            approveApplication(id);
        });
    }

    private void loadApplications() {
        appList.clear();
        appIds.clear();

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id, student_id, org_name, status FROM applications WHERE status='Pending'", null);

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(0);
                int studentId = cursor.getInt(1);
                String org = cursor.getString(2);
                String status = cursor.getString(3);

                appList.add("Student #" + studentId + " – " + org + " (" + status + ")");
                appIds.add(id);
            } while (cursor.moveToNext());
        }

        cursor.close();

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, appList);
        lvApplications.setAdapter(adapter);
    }

    private void approveApplication(int appId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("status", "Approved");
        db.update("applications", cv, "id=?", new String[]{String.valueOf(appId)});

        Toast.makeText(this, "Application approved", Toast.LENGTH_SHORT).show();
        loadApplications();
    }
}
