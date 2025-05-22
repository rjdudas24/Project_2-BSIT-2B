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
import java.util.Arrays;
import java.util.List;

public class DashboardStudentwOrgActivity extends Activity {

    private static final String TAG = "DashboardStudentOrg";
    LinearLayout layoutInquire, layoutTrackDocuments, layoutPostNotice, layoutTransactionHistory;
    LinearLayout layoutPostAccountability, layoutManageAccountabilities; // New accountability management
    TextView tvWelcome, tvOrganizationValue, tvPositionValue;
    boolean hasOrg;
    String orgPosition = "";
    int studentId = -1;
    DBHelper dbHelper;

    // Authorized positions for accountability management
    private static final List<String> ACCOUNTABILITY_OFFICERS = Arrays.asList(
            "Treasurer",
            "Associate Treasurer",
            "Auditor"
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard_student_org);

        try {
            // Get data from intent
            hasOrg = getIntent().getBooleanExtra("hasOrg", false);
            studentId = getIntent().getIntExtra("studentId", -1);

            Log.d(TAG, "Student ID: " + studentId + ", hasOrg: " + hasOrg);

            if (studentId == -1) {
                Log.e(TAG, "No valid student ID received");
                Toast.makeText(this, "Error: No valid student ID", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            // Initialize dbHelper
            dbHelper = new DBHelper(this);

            // Initialize views
            initializeViews();

            // Get org position if available
            if (hasOrg && studentId != -1) {
                loadOrgPosition();
            }

            // Show organization-specific UI elements
            setupVisibility();

            // Set click listeners
            setupClickListeners();

        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage(), e);
            Toast.makeText(this, "Error initializing dashboard: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initializeViews() {
        tvOrganizationValue = findViewById(R.id.tvOrganizationValue);
        tvPositionValue = findViewById(R.id.tvPositionValue);
        layoutInquire = findViewById(R.id.layoutFacultyInquiry);
        // layoutAccountabilities removed - org officers don't pay fees
        layoutTrackDocuments = findViewById(R.id.layoutTrackDocuments);
        layoutPostNotice = findViewById(R.id.layoutPostNotice);
        layoutTransactionHistory = findViewById(R.id.layoutTransactionHistory);

        // New accountability management layouts
        layoutPostAccountability = findViewById(R.id.layoutPostAccountability);
        layoutManageAccountabilities = findViewById(R.id.layoutManageAccountabilities);
    }

    private void setupVisibility() {
        // Show organization-specific UI elements
        if (layoutTrackDocuments != null) {
            layoutTrackDocuments.setVisibility(hasOrg ? View.VISIBLE : View.GONE);
        }
        if (layoutPostNotice != null) {
            layoutPostNotice.setVisibility(hasOrg ? View.VISIBLE : View.GONE);
        }

        // Show accountability management only for authorized officers
        boolean isAccountabilityOfficer = ACCOUNTABILITY_OFFICERS.contains(orgPosition);

        if (layoutPostAccountability != null) {
            layoutPostAccountability.setVisibility(isAccountabilityOfficer ? View.VISIBLE : View.GONE);
        }
        if (layoutManageAccountabilities != null) {
            layoutManageAccountabilities.setVisibility(isAccountabilityOfficer ? View.VISIBLE : View.GONE);
        }

        // Show position if student is part of an organization
        if (hasOrg && !orgPosition.isEmpty()) {
            if (tvPositionValue != null) {
                tvPositionValue.setText(orgPosition);
            }
        }
    }

    private void setupClickListeners() {
        // Faculty Inquiry
        if (layoutInquire != null) {
            layoutInquire.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        Intent intent = new Intent(DashboardStudentwOrgActivity.this, FacultyInquiryActivity.class);
                        intent.putExtra("studentId", studentId);
                        startActivity(intent);
                        Log.d(TAG, "Launched Faculty Inquiry with studentId: " + studentId);
                    } catch (Exception e) {
                        Log.e(TAG, "Error launching Faculty Inquiry: " + e.getMessage(), e);
                        Toast.makeText(DashboardStudentwOrgActivity.this, "Error opening Faculty Inquiry", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        // Accountabilities - Removed for org students as they don't pay org fees
        // Organization officers manage fees but don't pay them

        // Track Documents (Organization-specific feature)
        if (hasOrg && layoutTrackDocuments != null) {
            layoutTrackDocuments.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(this, TrackDocumentsActivity.class);
                    intent.putExtra("position", orgPosition);
                    intent.putExtra("studentId", studentId);
                    startActivity(intent);
                    Log.d(TAG, "Launched Track Documents with position: " + orgPosition + ", studentId: " + studentId);
                } catch (Exception e) {
                    Log.e(TAG, "Error launching Track Documents: " + e.getMessage(), e);
                    Toast.makeText(this, "Error opening document tracking: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Post Notice (Organization-specific feature)
        if (hasOrg && layoutPostNotice != null) {
            layoutPostNotice.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(this, PostNoticeActivity.class);
                    intent.putExtra("studentId", studentId);
                    intent.putExtra("position", orgPosition);
                    startActivity(intent);
                    Log.d(TAG, "Launched Post Notice with studentId: " + studentId + ", position: " + orgPosition);
                } catch (Exception e) {
                    Log.e(TAG, "Error launching Post Notice: " + e.getMessage(), e);
                    Toast.makeText(this, "Error opening Post Notice: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Transaction History
        if (layoutTransactionHistory != null) {
            layoutTransactionHistory.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(this, StudentTransactionHistoryActivity.class);
                    intent.putExtra("studentId", studentId);
                    startActivity(intent);
                    Log.d(TAG, "Launched Transaction History with studentId: " + studentId);
                } catch (Exception e) {
                    Log.e(TAG, "Error launching Transaction History: " + e.getMessage(), e);
                    Toast.makeText(this, "Error opening Transaction History", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Post Accountability (For authorized officers only)
        if (layoutPostAccountability != null) {
            layoutPostAccountability.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(this, PostAccountabilityActivity.class);
                    intent.putExtra("studentId", studentId);
                    intent.putExtra("position", orgPosition);
                    startActivity(intent);
                    Log.d(TAG, "Launched Post Accountability with studentId: " + studentId + ", position: " + orgPosition);
                } catch (Exception e) {
                    Log.e(TAG, "Error launching Post Accountability: " + e.getMessage(), e);
                    Toast.makeText(this, "Error opening Post Accountability: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Manage Accountabilities (For authorized officers only)
        if (layoutManageAccountabilities != null) {
            layoutManageAccountabilities.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(this, ManageAccountabilitiesActivity.class);
                    intent.putExtra("studentId", studentId);
                    intent.putExtra("position", orgPosition);
                    startActivity(intent);
                    Log.d(TAG, "Launched Manage Accountabilities with studentId: " + studentId + ", position: " + orgPosition);
                } catch (Exception e) {
                    Log.e(TAG, "Error launching Manage Accountabilities: " + e.getMessage(), e);
                    Toast.makeText(this, "Error opening Manage Accountabilities: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void loadOrgPosition() {
        SQLiteDatabase db = null;
        Cursor cursor = null;

        try {
            db = dbHelper.getReadableDatabase();
            cursor = db.rawQuery(
                    "SELECT org_role, name FROM users WHERE id = ?",
                    new String[]{String.valueOf(studentId)}
            );

            if (cursor.moveToFirst()) {
                orgPosition = cursor.getString(0);
                String name = cursor.getString(1);

                // Handle null or empty position
                if (orgPosition == null || orgPosition.isEmpty()) {
                    orgPosition = "Member";
                }

                // Set organization name and position in the UI
                if (tvOrganizationValue != null) {
                    tvOrganizationValue.setText("CSCo"); // You might want to replace this with organization name from DB
                }

                if (tvPositionValue != null) {
                    tvPositionValue.setText(orgPosition);
                }

                Log.d(TAG, "Loaded org position: " + orgPosition + " for user: " + name);

                // Update visibility after loading position
                setupVisibility();
            } else {
                Log.w(TAG, "No user found with ID: " + studentId);
                orgPosition = "Member";
                if (tvPositionValue != null) {
                    tvPositionValue.setText(orgPosition);
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Error loading org position: " + e.getMessage(), e);
            orgPosition = "Member";
            if (tvPositionValue != null) {
                tvPositionValue.setText(orgPosition);
            }
            Toast.makeText(this, "Error loading organization details", Toast.LENGTH_SHORT).show();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (db != null) {
                db.close();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning to this screen
        if (hasOrg && studentId != -1) {
            loadOrgPosition();
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