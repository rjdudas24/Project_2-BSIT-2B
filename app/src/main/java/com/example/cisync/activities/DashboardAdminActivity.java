package com.example.cisync.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import com.example.cisync.R;
import com.google.android.material.card.MaterialCardView;

public class DashboardAdminActivity extends Activity {

    MaterialCardView cardUsers, cardOrgRoster, cardTransactions, cardVerifyApplications;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard_admin);

        // Find views
        cardUsers = findViewById(R.id.cardUsers);
        cardOrgRoster = findViewById(R.id.cardOrgRoster);
        cardTransactions = findViewById(R.id.cardTransactions);
        cardVerifyApplications = findViewById(R.id.cardVerifyApplications);

        // Set click listeners
        cardUsers.setOnClickListener(v -> {
            // Navigate to AdminRosterActivity without specifying a role
            // The activity will handle showing both student and faculty in tabs
            Intent intent = new Intent(this, AdminRosterActivity.class);
            startActivity(intent);
        });

        cardOrgRoster.setOnClickListener(v -> {
            Intent intent = new Intent(this, AdminRosterActivity.class);
            intent.putExtra("role", "Organization");
            startActivity(intent);
        });

        cardTransactions.setOnClickListener(v ->
                startActivity(new Intent(this, AdminTransactionsActivity.class)));

        cardVerifyApplications.setOnClickListener(v ->
                startActivity(new Intent(this, AdminVerifyApplicationsActivity.class)));
    }
}