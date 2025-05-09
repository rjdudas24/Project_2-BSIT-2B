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
import java.util.Date;
import java.util.Locale;

public class PostNoticeActivity extends Activity {

    EditText etNotice;
    TextView tvCurrentPosition;
    Button btnSubmit;
    DBHelper dbHelper;
    String userPosition = "";
    int studentId = -1; // Default invalid value

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_notice);

        etNotice = findViewById(R.id.etNoticeContent);
        tvCurrentPosition = findViewById(R.id.tvCurrentPosition);
        btnSubmit = findViewById(R.id.btnPostNotice);
        dbHelper = new DBHelper(this);

        // Get studentId from intent
        studentId = getIntent().getIntExtra("studentId", -1);

        // Get current user's org position
        getUserPosition();

        btnSubmit.setOnClickListener(v -> {
            String content = etNotice.getText().toString().trim();
            if (content.isEmpty()) {
                Toast.makeText(this, "Please enter notice content", Toast.LENGTH_SHORT).show();
                return;
            }

            // Format: [Position] Notice content
            String formattedContent = "[" + userPosition + "] " + content;

            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put("student_id", studentId);
            cv.put("content", formattedContent);

            // Add timestamp to notices
            cv.put("timestamp", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));

            long result = db.insert("notices", null, cv);

            if (result != -1) {
                Toast.makeText(this, "Notice posted!", Toast.LENGTH_SHORT).show();
                etNotice.setText("");
            } else {
                Toast.makeText(this, "Failed to post notice.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void getUserPosition() {
        // Only proceed if we have a valid student ID
        if (studentId == -1) {
            tvCurrentPosition.setText("Error: Invalid user");
            btnSubmit.setEnabled(false);
            return;
        }

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT org_role FROM users WHERE id = ?",
                new String[]{String.valueOf(studentId)}
        );

        if (cursor.moveToFirst()) {
            userPosition = cursor.getString(0);
            tvCurrentPosition.setText("Posting as: " + userPosition);
        } else {
            userPosition = "Member";
            tvCurrentPosition.setText("Posting as: Member");
        }

        cursor.close();
    }
}