package com.example.cisync.activities;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.example.cisync.R;
import com.example.cisync.database.DBHelper;

public class DashboardFacultyActivity extends Activity {
    private static final String TAG = "DashboardFaculty";
    LinearLayout layoutNotifications, layoutHistory;
    Button btnLogoutFaculty;
    int facultyId; // Store the faculty ID

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard_faculty);

        // Get faculty ID from intent
        facultyId = getIntent().getIntExtra("userId", -1);
        if (facultyId == -1) {
            Log.w(TAG, "No valid faculty ID found in intent");
            // You may want to handle this case, or use a default ID for demo purposes
            facultyId = 1; // Default demo ID
        }

        Log.d(TAG, "Faculty ID: " + facultyId);

        // Initialize views
        layoutNotifications = findViewById(R.id.layoutFacultyNotifications);
        layoutHistory = findViewById(R.id.layoutFacultyHistory);
        btnLogoutFaculty = findViewById(R.id.btnLogoutFaculty);

        // Set up logout button
        if (btnLogoutFaculty != null) {
            btnLogoutFaculty.setOnClickListener(v -> {
                LogoutUtil.showLogoutDialog(this, facultyId);
            });
        }

        // Set click listeners for dashboard options
        layoutNotifications.setOnClickListener(v -> {
            Intent intent = new Intent(this, FacultyNotificationsActivity.class);
            intent.putExtra("userId", facultyId); // Pass the faculty ID
            startActivity(intent);
        });

        layoutHistory.setOnClickListener(v -> {
            Intent intent = new Intent(this, FacultyHistoryActivity.class);
            intent.putExtra("userId", facultyId); // Pass the faculty ID
            startActivity(intent);
            Log.d(TAG, "Starting FacultyHistoryActivity with facultyId: " + facultyId);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Record logout when activity is destroyed (user exits app)
        if (isFinishing() && facultyId != -1) {
            try {
                LoginActivity.recordLogoutSession(this, facultyId);
            } catch (Exception e) {
                Log.e(TAG, "Error recording logout: " + e.getMessage(), e);
            }
        }
    }
}