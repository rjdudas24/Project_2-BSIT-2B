package com.example.cisync.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import com.example.cisync.R;
import com.google.android.material.card.MaterialCardView;

public class DashboardAdminActivity extends Activity {

    private static final String TAG = "DashboardAdmin";
    MaterialCardView cardUsers, cardTransactions, cardVerifyApplications, cardLoginHistory;
    TextView tvWelcomeMessage;
    int adminId; // Store the admin user ID

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard_admin);

        // Get the admin ID from the intent if available
        adminId = getIntent().getIntExtra("userId", -1);
        Log.d(TAG, "Admin ID: " + adminId);

        // Find views
        initializeViews();

        // Set up personalized welcome message
        setupWelcomeMessage();

        // Set up click listeners
        setupClickListeners();
    }

    private void initializeViews() {
        try {
            // Card views
            cardUsers = findViewById(R.id.cardUsers);
            cardVerifyApplications = findViewById(R.id.cardVerifyApplications);
            cardTransactions = findViewById(R.id.cardTransactions);
            cardLoginHistory = findViewById(R.id.cardLoginHistory);

            // Welcome message
            tvWelcomeMessage = findViewById(R.id.tvWelcomeMessage);

        } catch (Exception e) {
            Log.e(TAG, "Error initializing views: " + e.getMessage(), e);
            Toast.makeText(this, "Error initializing dashboard", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupWelcomeMessage() {
        if (tvWelcomeMessage != null && adminId != -1) {
            try {
                // You could fetch the admin's name from the database here
                // For now, we'll just use a generic welcome message
                tvWelcomeMessage.setText("Welcome, Admin! [ID: " + adminId + "]");
            } catch (Exception e) {
                Log.e(TAG, "Error setting welcome message: " + e.getMessage(), e);
            }
        }
    }

    private void setupClickListeners() {
        // Users card - Navigate to AdminRosterActivity
        if (cardUsers != null) {
            cardUsers.setOnClickListener(v -> {
                try {
                    // Navigate to AdminRosterActivity without specifying a role
                    Intent intent = new Intent(this, AdminRosterActivity.class);
                    intent.putExtra("adminId", adminId); // Pass admin ID if needed
                    startActivity(intent);
                    Log.d(TAG, "Launching AdminRosterActivity");
                } catch (Exception e) {
                    Log.e(TAG, "Error launching AdminRosterActivity: " + e.getMessage(), e);
                    Toast.makeText(this, "Error opening user roster", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Verify Applications card
        if (cardVerifyApplications != null) {
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
        }

        // Transactions card
        if (cardTransactions != null) {
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

        // Login History card
        if (cardLoginHistory != null) {
            cardLoginHistory.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(this, AdminLoginHistoryActivity.class);
                    intent.putExtra("adminId", adminId);
                    startActivity(intent);
                    Log.d(TAG, "Launching AdminLoginHistoryActivity");
                } catch (Exception e) {
                    Log.e(TAG, "Error launching AdminLoginHistoryActivity: " + e.getMessage(), e);
                    Toast.makeText(this, "Error opening login history", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Log.w(TAG, "Login history card not found in layout");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Record logout when activity is destroyed (user exits app)
        if (isFinishing() && adminId != -1) {
            try {
                LoginActivity.recordLogoutSession(this, adminId);
            } catch (Exception e) {
                Log.e(TAG, "Error recording logout: " + e.getMessage(), e);
            }
        }
    }
}