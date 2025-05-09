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

    ListView lvAccountabilities;
    DBHelper dbHelper;
    ArrayList<String> accountList = new ArrayList<>();
    ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_accountabilities);

        lvAccountabilities = findViewById(R.id.lvAccountabilities);
        dbHelper = new DBHelper(this);

        loadAccountabilities();
    }

    private void loadAccountabilities() {
        accountList.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // For demo: static student_id = 1
        Cursor cursor = db.rawQuery("SELECT description FROM accountabilities WHERE student_id=?", new String[]{"1"});

        if (cursor.moveToFirst()) {
            do {
                accountList.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }

        cursor.close();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, accountList);
        lvAccountabilities.setAdapter(adapter);
    }
}
