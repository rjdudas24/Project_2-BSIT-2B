package com.example.cisync.activities;

import android.app.Activity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.*;
import com.example.cisync.R;
import com.example.cisync.database.DBHelper;

import java.util.ArrayList;

public class AdminRosterActivity extends Activity {

    ListView lvRoster;
    DBHelper dbHelper;
    ArrayList<String> rosterList = new ArrayList<>();
    ArrayAdapter<String> adapter;
    String roleToView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_roster);

        roleToView = getIntent().getStringExtra("role");
        lvRoster = findViewById(R.id.lvRoster);
        dbHelper = new DBHelper(this);

        loadRoster(roleToView);
    }

    private void loadRoster(String role) {
        rosterList.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor;
        if (role.equals("Organization")) {
            cursor = db.rawQuery("SELECT name, email FROM users WHERE role='Student' AND has_org=1", null);
        } else {
            cursor = db.rawQuery("SELECT name, email FROM users WHERE role=?", new String[]{role});
        }

        if (cursor.moveToFirst()) {
            do {
                rosterList.add(cursor.getString(0) + " - " + cursor.getString(1));
            } while (cursor.moveToNext());
        }

        cursor.close();

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, rosterList);
        lvRoster.setAdapter(adapter);
    }
}
