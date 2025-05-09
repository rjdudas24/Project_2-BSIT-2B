package com.example.cisync.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import com.example.cisync.R;

public class DashboardAdminActivity extends Activity {

    LinearLayout layoutStudent, layoutFaculty, layoutOrg, layoutTx, layoutApps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard_admin);

        layoutStudent = findViewById(R.id.layoutStudentRoster);
        layoutFaculty = findViewById(R.id.layoutFacultyRoster);
        layoutOrg = findViewById(R.id.layoutOrgRoster);
        layoutTx = findViewById(R.id.layoutAdminTransactions);
        layoutApps = findViewById(R.id.layoutVerifyApplications);

        layoutStudent.setOnClickListener(v -> openRoster("Student"));
        layoutFaculty.setOnClickListener(v -> openRoster("Faculty"));
        layoutOrg.setOnClickListener(v -> openRoster("Organization"));
        layoutTx.setOnClickListener(v -> startActivity(new Intent(this, AdminTransactionsActivity.class)));
        layoutApps.setOnClickListener(v -> startActivity(new Intent(this, AdminVerifyApplicationsActivity.class)));
    }

    private void openRoster(String role) {
        Intent intent = new Intent(this, AdminRosterActivity.class);
        intent.putExtra("role", role);
        startActivity(intent);
    }
}
