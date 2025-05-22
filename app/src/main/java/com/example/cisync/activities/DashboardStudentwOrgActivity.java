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

public class DashboardStudentwOrgActivity extends Activity {

    private static final String TAG = "DashboardStudentOrg";
    LinearLayout layoutInquire, layoutAccountabilities, layoutTrackDocuments, layoutPostNotice, layoutTransactionHistory;
    TextView tvWelcome, tvOrganizationValue, tvPositionValue;
    boolean hasOrg;
    String orgPosition = "";
    int studentId = -1;
    DBHelper dbHelper;

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
            tvOrganizationValue = findViewById(R.id.tvOrganizationValue);
            tvPositionValue = findViewById(R.id.tvPositionValue);
            layoutInquire = findViewById(R.id.layoutFacultyInquiry);
            layoutAccountabilities = findViewById(R.id.layoutAccountabilities);
            layoutTrackDocuments = findViewById(R.id.layoutTrackDocuments);
            layoutPostNotice = findViewById(R.id.layoutPostNotice);

            // Replace View Notices with Transaction History
            layoutTransactionHistory = findViewById(R.id.layoutTransactionHistory);

            // Get org position if available
            if (hasOrg && studentId != -1) {
                loadOrgPosition();
            }

            // Show organization-specific UI elements
            if (layoutTrackDocuments != null) {
                layoutTrackDocuments.setVisibility(hasOrg ? View.VISIBLE : View.GONE);
            }
            if (layoutPostNotice != null) {
                layoutPostNotice.setVisibility(hasOrg ? View.VISIBLE : View.GONE);
            }

            // Show position if student is part of an organization
            if (hasOrg && !orgPosition.isEmpty()) {
                if (tvPositionValue != null) {
                    tvPositionValue.setText(orgPosition);
                }
            }

            // Set click listeners
            setupClickListeners();

        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage(), e);
            Toast.makeText(this, "Error initializing dashboard: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
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

        // Accountabilities
        if (layoutAccountabilities != null) {
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
        }

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

        // Transaction History (Replaces View Notices)
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
        } else {
            Log.w(TAG, "Transaction History layout not found - feature not available in this layout");
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