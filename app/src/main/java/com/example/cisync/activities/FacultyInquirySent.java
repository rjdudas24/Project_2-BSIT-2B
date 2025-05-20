package com.example.cisync.activities;

import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cisync.R;
import com.example.cisync.database.DBHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FacultyInquiryActivity extends AppCompatActivity {

    private EditText etFacultyName;
    private Spinner spDepartment;
    private EditText etSubject;
    private EditText etDescription;
    private Button btnSubmit;
    private DBHelper dbHelper;
    private int studentId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faculty_inquiry);

        // Initialize database helper
        dbHelper = new DBHelper(this);

        // Get student ID from intent
        studentId = getIntent().getIntExtra("studentId", -1);

        // Initialize UI components
        etFacultyName = findViewById(R.id.etFacultyName);
        spDepartment = findViewById(R.id.spDepartment);
        etSubject = findViewById(R.id.etSubject);
        etDescription = findViewById(R.id.etDescription);
        btnSubmit = findViewById(R.id.btnSubmit);

        btnSubmit.setOnClickListener(v -> {
            if (validateInputs()) {
                if (submitInquiry()) {
                    // Show confirmation screen
                    Intent intent = new Intent(FacultyInquiryActivity.this, FacultyInquirySent.class);
                    startActivity(intent);
                    finish(); // Close this activity
                }
            }
        });
    }

    private boolean validateInputs() {
        String facultyName = etFacultyName.getText().toString().trim();
        String subject = etSubject.getText().toString().trim();
        String description = etDescription.getText().toString().trim();

        if (facultyName.isEmpty()) {
            etFacultyName.setError("Please enter faculty name");
            return false;
        }

        if (subject.isEmpty()) {
            etSubject.setError("Please enter subject");
            return false;
        }

        if (description.isEmpty()) {
            etDescription.setError("Please enter description");
            return false;
        }

        return true;
    }

    private boolean submitInquiry() {
        // Only proceed if we have a valid student ID
        if (studentId == -1) {
            Toast.makeText(this, "Error: Invalid user ID", Toast.LENGTH_SHORT).show();
            return false;
        }

        try {
            String facultyName = etFacultyName.getText().toString().trim();
            String department = spDepartment.getSelectedItem().toString();
            String subject = etSubject.getText().toString().trim();
            String description = etDescription.getText().toString().trim();
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put("student_id", studentId);
            values.put("faculty_name", facultyName);
            values.put("department", department);
            values.put("subject", subject);
            values.put("description", description);
            values.put("status", "Pending");  // Initial status
            values.put("created_at", timestamp);

            long result = db.insert("faculty_inquiries", null, values);
            db.close();

            if (result == -1) {
                Toast.makeText(this, "Failed to send inquiry", Toast.LENGTH_SHORT).show();
                return false;
            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Database error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return false;
        }
    }
}