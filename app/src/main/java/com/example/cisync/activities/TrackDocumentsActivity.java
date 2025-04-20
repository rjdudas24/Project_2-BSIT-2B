package com.example.cisync.activities;

import android.app.Activity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.*;
import com.example.cisync.R;
import com.example.cisync.database.DBHelper;

import java.util.ArrayList;

public class TrackDocumentsActivity extends Activity {

    ListView lvDocuments;
    DBHelper dbHelper;
    ArrayList<String> docList = new ArrayList<>();
    ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_documents);

        lvDocuments = findViewById(R.id.lvDocuments);
        dbHelper = new DBHelper(this);

        loadDocuments();
    }

    private void loadDocuments() {
        docList.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT status FROM documents WHERE student_id=?", new String[]{"1"});

        if (cursor.moveToFirst()) {
            do {
                docList.add("Status: " + cursor.getString(0));
            } while (cursor.moveToNext());
        }

        cursor.close();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, docList);
        lvDocuments.setAdapter(adapter);
    }
}
