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
    LinearLayout layoutInquire, layoutAccountabilities, layoutViewNotices, layoutViewNotifications;
    TextView tvWelcome, tvUsername, tvNotificationBadge;
    Button btnLogoutStudent;
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
            initializeViews();

            // Load username from database
            loadUsername();

            // Update notification badge
            updateNotificationBadge();

            // Set up click listeners
            setupClickListeners();

        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage(), e);
            Toast.makeText(this, "Error initializing dashboard: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initializeViews() {
        tvWelcome = findViewById(R.id.tvWelcome);
        tvUsername = findViewById(R.id.tvUsername);
        layoutInquire = findViewById(R.id.layoutFacultyInquiry);
        layoutAccountabilities = findViewById(R.id.layoutAccountabilities);
        btnLogoutStudent = findViewById(R.id.btnLogoutStudent);

        // Try to find View Notices layout (may not exist in older layouts)
        layoutViewNotices = findViewById(R.id.layoutViewNotices);

        // Try to find View Notifications layout
        layoutViewNotifications = findViewById(R.id.layoutViewNotifications);

        // Try to find notification badge
        tvNotificationBadge = findViewById(R.id.tvNotificationBadge);
    }

    private void setupClickListeners() {
        // Logout button
        if (btnLogoutStudent != null) {
            btnLogoutStudent.setOnClickListener(v -> {
                LogoutUtil.showLogoutDialog(this, studentId);
            });
        }

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

        // View Notifications (new feature)
        if (layoutViewNotifications != null) {
            layoutViewNotifications.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(this, StudentNotificationsActivity.class);
                    intent.putExtra("studentId", studentId);
                    startActivity(intent);
                    Log.d(TAG, "Launched Student Notifications with studentId: " + studentId);
                } catch (Exception e) {
                    Log.e(TAG, "Error launching Student Notifications: " + e.getMessage(), e);
                    Toast.makeText(this, "Error opening Notifications", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Log.w(TAG, "View Notifications layout not found - notifications feature not available in this layout");
        }
    }

    private void updateNotificationBadge() {
        try {
            SQLiteDatabase db = dbHelper.getReadableDatabase();

            // Count unread notifications for this student
            Cursor cursor = db.rawQuery(
                    "SELECT COUNT(*) FROM transactions WHERE user_id = ? AND COALESCE(read_status, 0) = 0",
                    new String[]{String.valueOf(studentId)}
            );

            int unreadCount = 0;
            if (cursor.moveToFirst()) {
                unreadCount = cursor.getInt(0);
            }
            cursor.close();

            // Update notification badge
            if (tvNotificationBadge != null) {
                if (unreadCount > 0) {
                    tvNotificationBadge.setText(String.valueOf(unreadCount));
                    tvNotificationBadge.setVisibility(View.VISIBLE);
                } else {
                    tvNotificationBadge.setVisibility(View.GONE);
                }
            }

            Log.d(TAG, "Updated notification badge: " + unreadCount + " unread notifications");

        } catch (Exception e) {
            Log.e(TAG, "Error updating notification badge: " + e.getMessage(), e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload username and update notification badge when returning to this activity
        loadUsername();
        updateNotificationBadge();
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

        // Record logout when activity is destroyed (user exits app)
        if (isFinishing() && studentId != -1) {
            try {
                LoginActivity.recordLogoutSession(this, studentId);
            } catch (Exception e) {
                Log.e(TAG, "Error recording logout: " + e.getMessage(), e);
            }
        }
    }
}