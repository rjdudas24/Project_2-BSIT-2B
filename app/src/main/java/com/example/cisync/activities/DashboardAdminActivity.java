package com.example.cisync.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import com.example.cisync.R;
import com.google.android.material.card.MaterialCardView;

public class DashboardAdminActivity extends Activity {

    private static final String TAG = "DashboardAdmin";
    MaterialCardView cardUsers, cardOrgRoster, cardTransactions, cardVerifyApplications;
    int adminId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard_admin);

        adminId = getIntent().getIntExtra("userId", -1);
        Log.d(TAG, "Admin ID: " + adminId);

        // Find views
        cardUsers = findViewById(R.id.cardUsers);
        cardVerifyApplications = findViewById(R.id.cardVerifyApplications);
        cardTransactions = findViewById(R.id.cardTransactions);

        // Set click listeners
        // Users card - Navigate to AdminRosterActivity
        cardUsers.setOnClickListener(v -> {
            try {
                // Navigate to AdminRosterActivity without specifying a role
                // The activity will handle showing both student and faculty in tabs
                Intent intent = new Intent(this, AdminRosterActivity.class);
                intent.putExtra("adminId", adminId); // Pass admin ID if needed
                startActivity(intent);
                Log.d(TAG, "Launching AdminRosterActivity");
            } catch (Exception e) {
                Log.e(TAG, "Error launching AdminRosterActivity: " + e.getMessage(), e);
                Toast.makeText(this, "Error opening user roster", Toast.LENGTH_SHORT).show();
            }
        });

        // Verify Applications card
        cardVerifyApplications.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(this, AdminVerifyApplicationsActivity.class);
                intent.putExtra("adminId", adminId); // Pass admin ID if needed
                startActivity(intent);
                Log.d(TAG, "Launching AdminVerifyApplicationsActivity");
            } catch (Exception e) {
                Log.e(TAG, "Error launching AdminVerifyApplicationsActivity: " + e.getMessage(), e);
                Toast.makeText(this, "Error opening verification panel", Toast.LENGTH_SHORT).show();
            }
        });

        // Transactions card
        cardTransactions.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(this, AdminTransactionsActivity.class);
                intent.putExtra("adminId", adminId); // Pass admin ID if needed
                startActivity(intent);
                Log.d(TAG, "Launching AdminTransactionsActivity");
            } catch (Exception e) {
                Log.e(TAG, "Error launching AdminTransactionsActivity: " + e.getMessage(), e);
                Toast.makeText(this, "Error opening transactions", Toast.LENGTH_SHORT).show();
            }
        });
    }
}