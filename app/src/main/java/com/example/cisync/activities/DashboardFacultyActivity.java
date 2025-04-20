package com.example.cisync.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Toast;
import com.example.cisync.R;

public class DashboardFacultyActivity extends Activity {

    LinearLayout layoutNotifications, layoutHistory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard_faculty);

        layoutNotifications = findViewById(R.id.layoutFacultyNotifications);
        layoutHistory = findViewById(R.id.layoutFacultyHistory);

        layoutNotifications.setOnClickListener(v -> startActivity(new Intent(this, FacultyNotificationsActivity.class)));
        layoutHistory.setOnClickListener(v -> startActivity(new Intent(this, FacultyHistoryActivity.class)));
    }
}
