package com.example.cisync.activities;

import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cisync.R;
import com.example.cisync.database.DBHelper;

import java.util.ArrayList;
import java.util.Arrays;

public class FacultyInquiryActivity extends AppCompatActivity {

    private ImageButton btnBack, btnClearFaculty;
    private EditText etFacultyName;
    private Spinner spinnerDepartment;
    private TextView tvPurposeTitle, tvPurposeDescription;
    private Button btnSubmit;

    private ArrayList<String> departments;
    private int studentId;
    private DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faculty_inquiry);

        studentId = getIntent().getIntExtra("studentId", -1);
        dbHelper = new DBHelper(this);

        initializeViews();
        setupListeners();
        setupDepartmentSpinner();
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        btnClearFaculty = findViewById(R.id.btnClearFaculty);
        etFacultyName = findViewById(R.id.etFacultyName);
        spinnerDepartment = findViewById(R.id.spinnerDepartment);
        tvPurposeTitle = findViewById(R.id.etPurposeTitle);
        tvPurposeDescription = findViewById(R.id.etPurposeDescription);
        btnSubmit = findViewById(R.id.btnSubmit);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> onBackPressed());

        btnClearFaculty.setOnClickListener(v -> etFacultyName.setText(""));

        btnSubmit.setOnClickListener(v -> {
            if (validateForm()) {
                submitInquiry();
            }
        });

        tvPurposeTitle.setOnClickListener(v -> showEditTitleDialog());
        tvPurposeDescription.setOnClickListener(v -> showEditDescriptionDialog());
    }

    private void setupDepartmentSpinner() {
        departments = new ArrayList<>(Arrays.asList(
                "Select Department",
                "Information Technology",
                "SDD"
        ));

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                departments
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinnerDepartment.setAdapter(adapter);
        spinnerDepartment.setSelection(0);
    }

    private boolean validateForm() {
        boolean isValid = true;

        if (etFacultyName.getText().toString().trim().isEmpty()) {
            etFacultyName.setError("Faculty name is required");
            isValid = false;
        }

        if (spinnerDepartment.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Please select a department", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        String title = tvPurposeTitle.getText().toString().trim();
        if (title.isEmpty() || title.equals("Enter Subject")) {
            Toast.makeText(this, "Please enter a subject for your inquiry", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        String description = tvPurposeDescription.getText().toString().trim();
        if (description.isEmpty() || description.equals("Write purpose...")) {
            Toast.makeText(this, "Please enter a description for your inquiry", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        return isValid;
    }

    private void submitInquiry() {
        String facultyName = etFacultyName.getText().toString().trim();
        String department = spinnerDepartment.getSelectedItem().toString();
        String purposeTitle = tvPurposeTitle.getText().toString().trim();
        String purposeDescription = tvPurposeDescription.getText().toString().trim();

        if (studentId == -1) {
            Toast.makeText(this, "Error: Student ID not found", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean success = saveInquiryToDatabase(facultyName, department, purposeTitle, purposeDescription);

        if (success) {
            Intent intent = new Intent(FacultyInquiryActivity.this, FacultyInquirySent.class);
            // Make sure to pass the studentId to the FacultyInquirySent activity
            intent.putExtra("studentId", studentId);
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(this, "Failed to submit inquiry. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean saveInquiryToDatabase(String facultyName, String department, String subject, String description) {
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            String sql = "INSERT INTO faculty_inquiries (student_id, faculty_name, department, subject, description, status, created_at) " +
                    "VALUES (?, ?, ?, ?, ?, 'Pending', datetime('now'))";

            db.execSQL(sql, new Object[]{
                    studentId,
                    facultyName,
                    department,
                    subject,
                    description
            });

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void showEditTitleDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        final EditText input = new EditText(this);
        String current = tvPurposeTitle.getText().toString();
        input.setText(current.equals("Enter Subject") ? "" : current);

        builder.setTitle("Enter Subject");
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String title = input.getText().toString().trim();
            tvPurposeTitle.setText(title.isEmpty() ? "Enter Subject" : title);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showEditDescriptionDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        final EditText input = new EditText(this);
        input.setMinLines(4);
        String current = tvPurposeDescription.getText().toString();
        input.setText(current.equals("Write purpose...") ? "" : current);

        builder.setTitle("Write Purpose");
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String description = input.getText().toString().trim();
            tvPurposeDescription.setText(description.isEmpty() ? "Write purpose..." : description);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }
}