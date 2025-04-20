package com.example.cisync.activities;

import android.app.Activity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.*;
import com.example.cisync.R;
import com.example.cisync.database.DBHelper;

import java.util.ArrayList;

public class AdminTransactionsActivity extends Activity {

    ListView lvAllTransactions;
    DBHelper dbHelper;
    ArrayList<String> txList = new ArrayList<>();
    ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_transactions);

        lvAllTransactions = findViewById(R.id.lvAllTransactions);
        dbHelper = new DBHelper(this);

        loadTransactions();
    }

    private void loadTransactions() {
        txList.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT message, timestamp FROM transactions", null);
        if (cursor.moveToFirst()) {
            do {
                txList.add(cursor.getString(1) + ": " + cursor.getString(0));
            } while (cursor.moveToNext());
        }
        cursor.close();

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, txList);
        lvAllTransactions.setAdapter(adapter);
    }
}
