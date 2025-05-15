package com.example.cisync.activities;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import com.example.cisync.R;
import com.example.cisync.database.DBHelper;

public class DashboardStudentActivity extends Activity {

    LinearLayout layoutInquire, layoutAccountabilities;
    TextView tvWelcome, tvUsername;
    int studentId;
    DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard_student);

        // Get data from intent
        studentId = getIntent().getIntExtra("studentId", -1);

        // Initialize dbHelper
        dbHelper = new DBHelper(this);

        // Initialize views
        tvWelcome = findViewById(R.id.tvWelcome);
        tvUsername = findViewById(R.id.tvUsername);
        layoutInquire = findViewById(R.id.layoutFacultyInquiry);
        layoutAccountabilities = findViewById(R.id.layoutAccountabilities);

        // Load username from database
        loadUsername();

        // Set click listeners
        layoutInquire.setOnClickListener(v ->
                Toast.makeText(this, "Inquire Faculty feature", Toast.LENGTH_SHORT).show()
        );

        layoutAccountabilities.setOnClickListener(v -> {
            Intent intent = new Intent(this, ViewAccountabilitiesActivity.class);
            intent.putExtra("studentId", studentId);
            startActivity(intent);
        });
    }

    private void loadUsername() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT name FROM users WHERE id = ?",
                new String[]{String.valueOf(studentId)}
        );

        if (cursor.moveToFirst()) {
            String name = cursor.getString(0);
            // Set username in welcome message
            if (tvUsername != null) {
                tvUsername.setText(name);
            }
        }

        cursor.close();
    }
}