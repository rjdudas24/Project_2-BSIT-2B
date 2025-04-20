package com.example.cisync.activities;

import android.app.Activity;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.*;
import com.example.cisync.R;
import com.example.cisync.database.DBHelper;

public class PostNoticeActivity extends Activity {

    EditText etNotice;
    Button btnSubmit;
    DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_notice);

        etNotice = findViewById(R.id.etNoticeContent);
        btnSubmit = findViewById(R.id.btnPostNotice);
        dbHelper = new DBHelper(this);

        btnSubmit.setOnClickListener(v -> {
            String content = etNotice.getText().toString().trim();
            if (content.isEmpty()) {
                Toast.makeText(this, "Please enter notice content", Toast.LENGTH_SHORT).show();
                return;
            }

            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put("student_id", 1); // static ID
            cv.put("content", content);
            long result = db.insert("notices", null, cv);

            if (result != -1) {
                Toast.makeText(this, "Notice posted!", Toast.LENGTH_SHORT).show();
                etNotice.setText("");
            } else {
                Toast.makeText(this, "Failed to post notice.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
