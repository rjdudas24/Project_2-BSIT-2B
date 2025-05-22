package com.example.cisync.activities;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;
import com.example.cisync.R;
import com.example.cisync.database.DBHelper;

public class DashboardStudentActivity extends Activity {

    private static final String TAG = "DashboardStudent";
    LinearLayout layoutInquire, layoutAccountabilities, layoutViewNotices;
    TextView tvWelcome, tvUsername;
    int studentId;
    DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard_student);

        try {
            // Get data from intent
            studentId = getIntent().getIntExtra("studentId", -1);

            if (studentId == -1) {
                Log.e(TAG, "No valid student ID received");
                Toast.makeText(this, "Error: No valid student ID", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            Log.d(TAG, "Student ID: " + studentId);

            // Initialize dbHelper
            dbHelper = new DBHelper(this);

            // Initialize views
            tvWelcome = findViewById(R.id.tvWelcome);
            tvUsername = findViewById(R.id.tvUsername);
            layoutInquire = findViewById(R.id.layoutFacultyInquiry);
            layoutAccountabilities = findViewById(R.id.layoutAccountabilities);

            // Try to find View Notices layout (may not exist in older layouts)
            layoutViewNotices = findViewById(R.id.layoutViewNotices);

            // Load username from database
            loadUsername();

            // Set up click listeners
            setupClickListeners();

        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage(), e);
            Toast.makeText(this, "Error initializing dashboard: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupClickListeners() {
        // Faculty Inquiry
        layoutInquire.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent intent = new Intent(DashboardStudentActivity.this, FacultyInquiryActivity.class);
                    intent.putExtra("studentId", studentId);
                    startActivity(intent);
                    Log.d(TAG, "Launched Faculty Inquiry with studentId: " + studentId);
                } catch (Exception e) {
                    Log.e(TAG, "Error launching Faculty Inquiry: " + e.getMessage(), e);
                    Toast.makeText(DashboardStudentActivity.this, "Error opening Faculty Inquiry", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Accountabilities
        layoutAccountabilities.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(this, ViewAccountabilitiesActivity.class);
                intent.putExtra("studentId", studentId);
                startActivity(intent);
                Log.d(TAG, "Launched Accountabilities with studentId: " + studentId);
            } catch (Exception e) {
                Log.e(TAG, "Error launching Accountabilities: " + e.getMessage(), e);
                Toast.makeText(this, "Error opening Accountabilities", Toast.LENGTH_SHORT).show();
            }
        });

        // View Notices (only if layout exists)
        if (layoutViewNotices != null) {
            layoutViewNotices.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(this, ViewNoticesActivity.class);
                    intent.putExtra("studentId", studentId);
                    startActivity(intent);
                    Log.d(TAG, "Launched View Notices with studentId: " + studentId);
                } catch (Exception e) {
                    Log.e(TAG, "Error launching View Notices: " + e.getMessage(), e);
                    Toast.makeText(this, "Error opening Notices", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Log.w(TAG, "View Notices layout not found - notices feature not available in this layout");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload username when returning to this activity
        loadUsername();
    }

    private void loadUsername() {
        SQLiteDatabase db = null;
        Cursor cursor = null;

        try {
            db = dbHelper.getReadableDatabase();
            cursor = db.rawQuery(
                    "SELECT name FROM users WHERE id = ?",
                    new String[]{String.valueOf(studentId)}
            );

            if (cursor.moveToFirst()) {
                String name = cursor.getString(0);
                // Set username in welcome message
                if (tvUsername != null) {
                    tvUsername.setText(name);
                }
                Log.d(TAG, "Loaded username: " + name);
            } else {
                Log.w(TAG, "No user found with ID: " + studentId);
                if (tvUsername != null) {
                    tvUsername.setText("Unknown User");
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Error loading username: " + e.getMessage(), e);
            if (tvUsername != null) {
                tvUsername.setText("Error Loading Name");
            }
        } finally {
            if (cursor != null) cursor.close();
            if (db != null) db.close();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}