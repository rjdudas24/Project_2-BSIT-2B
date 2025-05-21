
package com.example.cisync.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cisync.R;
import com.example.cisync.database.DBHelper;

/**
 * Activity displayed after a faculty inquiry has been successfully submitted.
 * Shows a confirmation message and provides an OK button to return to previous screen.
 */
public class FacultyInquirySent extends AppCompatActivity {

    private Button btnOk;
    private DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faculty_inquiry_sent);

        // Initialize database helper
        dbHelper = new DBHelper(this);

        // Initialize UI components
        btnOk = findViewById(R.id.btnOk);

        // Set click listener for OK button
        btnOk.setOnClickListener(v -> {
            // Record transaction in database (optional)
            recordTransaction();

            // Return to main activity or appropriate screen
            Intent intent = new Intent(FacultyInquirySent.this, DashboardStudentActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); // Clear activity stack
            startActivity(intent);
            finish(); // Close this activity
        });
    }

    /**
     * Records the inquiry submission as a transaction in the database
     * This is optional but provides a record of user activity
     */
    private void recordTransaction() {
        try {
            // Get student ID if passed from previous activity
            int studentId = getIntent().getIntExtra("studentId", -1);

            if (studentId != -1) {
                // Insert directly into transactions table
                long timestamp = System.currentTimeMillis();
                dbHelper.getWritableDatabase().execSQL(
                        "INSERT INTO transactions (user_id, action_type, description, timestamp) VALUES (?, ?, ?, ?)",
                        new Object[]{studentId, "Faculty Inquiry", "Faculty inquiry submitted successfully", timestamp}
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
            // No need to show error to user since this is just logging
        }
    }
}