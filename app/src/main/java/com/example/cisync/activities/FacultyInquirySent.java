package com.example.cisync.activities;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cisync.R;
import com.example.cisync.database.DBHelper;

/**
 * Activity displayed after a faculty inquiry has been successfully submitted.
 * Shows a confirmation message and provides an OK button to return to appropriate dashboard.
 */
public class FacultyInquirySent extends AppCompatActivity {

    private static final String TAG = "FacultyInquirySent";
    private Button btnOk;
    private DBHelper dbHelper;
    private int studentId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faculty_inquiry_sent);

        // Initialize database helper
        dbHelper = new DBHelper(this);

        // Get student ID from intent
        studentId = getIntent().getIntExtra("studentId", -1);
        Log.d(TAG, "Received student ID: " + studentId);

        // Initialize UI components
        btnOk = findViewById(R.id.btnOk);

        // Set click listener for OK button
        btnOk.setOnClickListener(v -> {
            // Record transaction in database
            recordTransaction();

            // Check if student has organization to determine correct dashboard
            redirectToCorrectDashboard();
        });
    }

    /**
     * Records the inquiry submission as a transaction in the database
     * This provides a record of user activity
     */
    private void recordTransaction() {
        try {
            if (studentId != -1) {
                SQLiteDatabase db = dbHelper.getWritableDatabase();

                // Insert transaction record for the student
                ContentValues transValues = new ContentValues();
                transValues.put("user_id", studentId);
                transValues.put("action_type", "Faculty Inquiry");
                transValues.put("description", "Faculty inquiry submitted successfully");
                transValues.put("timestamp", System.currentTimeMillis());

                long result = db.insert("transactions", null, transValues);

                if (result != -1) {
                    Log.d(TAG, "Transaction recorded successfully for student ID: " + studentId);
                } else {
                    Log.e(TAG, "Failed to record transaction for student ID: " + studentId);
                }

                db.close();
            } else {
                Log.e(TAG, "Cannot record transaction - invalid student ID: " + studentId);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error recording transaction: " + e.getMessage(), e);
            // No need to show error to user since this is just logging
        }
    }

    /**
     * Redirects user to the appropriate dashboard based on their organization status
     */
    private void redirectToCorrectDashboard() {
        try {
            if (studentId == -1) {
                Log.e(TAG, "Invalid student ID, cannot redirect");
                finish();
                return;
            }

            SQLiteDatabase db = dbHelper.getReadableDatabase();

            // Check if student has organization
            Cursor cursor = db.rawQuery(
                    "SELECT has_org FROM users WHERE id = ?",
                    new String[]{String.valueOf(studentId)}
            );

            boolean hasOrg = false;
            if (cursor.moveToFirst()) {
                hasOrg = cursor.getInt(0) == 1;
            }
            cursor.close();
            db.close();

            Intent intent;
            if (hasOrg) {
                // Student has organization - redirect to org dashboard
                intent = new Intent(FacultyInquirySent.this, DashboardStudentwOrgActivity.class);
                intent.putExtra("hasOrg", true);
                Log.d(TAG, "Redirecting to organization student dashboard");
            } else {
                // Regular student - redirect to regular dashboard
                intent = new Intent(FacultyInquirySent.this, DashboardStudentActivity.class);
                Log.d(TAG, "Redirecting to regular student dashboard");
            }

            // Pass back the student ID
            intent.putExtra("studentId", studentId);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); // Clear activity stack
            startActivity(intent);
            finish(); // Close this activity

        } catch (Exception e) {
            Log.e(TAG, "Error redirecting to dashboard: " + e.getMessage(), e);

            // Fallback - try to go to regular dashboard
            Intent fallbackIntent = new Intent(FacultyInquirySent.this, DashboardStudentActivity.class);
            fallbackIntent.putExtra("studentId", studentId);
            fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(fallbackIntent);
            finish();
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