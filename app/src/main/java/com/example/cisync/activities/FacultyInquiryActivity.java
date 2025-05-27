package com.example.cisync.activities;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
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

public class FacultyInquiryActivity extends AppCompatActivity {

    private ImageButton btnBack, btnClearFaculty;
    private EditText etFacultyName;
    private Spinner spinnerDepartment, spinnerFacultyList;
    private TextView tvPurposeTitle, tvPurposeDescription;
    private Button btnSubmit;

    private ArrayList<String> departments;
    private ArrayList<String> facultyNames = new ArrayList<>();
    private ArrayList<Integer> facultyIds = new ArrayList<>();
    private int selectedFacultyId = -1;
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
        loadFacultyList();
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        btnClearFaculty = findViewById(R.id.btnClearFaculty);
        etFacultyName = findViewById(R.id.etFacultyName);
        spinnerDepartment = findViewById(R.id.spinnerDepartment);
        spinnerFacultyList = findViewById(R.id.spinnerFacultyList); // New spinner for faculty selection
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

        // Faculty selection listener
        spinnerFacultyList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) { // Skip "Select Faculty" option
                    selectedFacultyId = facultyIds.get(position);
                    etFacultyName.setText(facultyNames.get(position));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedFacultyId = -1;
            }
        });
    }

    private void setupDepartmentSpinner() {
        departments = new ArrayList<>();
        departments.add("Select Department");
        departments.add("Information Technology");
        departments.add("SDD");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                R.layout.custom_spinner_white,
                departments
        );
        adapter.setDropDownViewResource(R.layout.custom_spinner_white);
        spinnerDepartment.setAdapter(adapter);
        spinnerDepartment.setSelection(0);
    }

    private void loadFacultyList() {
        facultyNames.clear();
        facultyIds.clear();

        // Add default option
        facultyNames.add("Select Faculty");
        facultyIds.add(-1);

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try {
            Cursor cursor = db.rawQuery(
                    "SELECT id, name FROM users WHERE role='Faculty' AND verified=1 ORDER BY name",
                    null
            );

            if (cursor.moveToFirst()) {
                do {
                    int id = cursor.getInt(0);
                    String name = cursor.getString(1);
                    facultyNames.add(name);
                    facultyIds.add(id);
                } while (cursor.moveToNext());
            }
            cursor.close();

            // Set up faculty spinner
            ArrayAdapter<String> facultyAdapter = new ArrayAdapter<>(
                    this,
                    R.layout.custom_spinner_white,
                    facultyNames
            );
            facultyAdapter.setDropDownViewResource(R.layout.custom_spinner_white);
            spinnerFacultyList.setAdapter(facultyAdapter);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error loading faculty list", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean validateForm() {
        boolean isValid = true;

        if (selectedFacultyId == -1) {
            Toast.makeText(this, "Please select a faculty member", Toast.LENGTH_SHORT).show();
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
            db.beginTransaction();
            Log.d("TRANSACTION_DEBUG", "Transaction started for studentId=" + studentId + ", facultyId=" + selectedFacultyId);


            // Insert faculty inquiry
            ContentValues inquiryValues = new ContentValues();
            inquiryValues.put("student_id", studentId);
            inquiryValues.put("faculty_id", selectedFacultyId);
            inquiryValues.put("faculty_name", facultyName);
            inquiryValues.put("department", department);
            inquiryValues.put("subject", subject);
            inquiryValues.put("description", description);
            inquiryValues.put("status", "Pending");
            inquiryValues.put("created_at", System.currentTimeMillis());

            long inquiryId = db.insert("faculty_inquiries", null, inquiryValues);
            Log.d("TRANSACTION_DEBUG", "Inserted faculty inquiry with ID: " + inquiryId);

            if (inquiryId != -1) {
                // Create notification for the selected faculty
                ContentValues facultyNotification = new ContentValues();
                facultyNotification.put("user_id", selectedFacultyId);
                facultyNotification.put("action_type", "Faculty Inquiry");
                facultyNotification.put("description", "New inquiry from student: " + subject);
                facultyNotification.put("timestamp", System.currentTimeMillis());
                facultyNotification.put("read_status", 0); // Unread
                facultyNotification.put("inquiry_id", inquiryId);

                long facultyTxId = db.insert("transactions", null, facultyNotification);
                Log.d("TRANSACTION_DEBUG", "Faculty transaction inserted. ID=" + facultyTxId +
                        ", user_id=" + selectedFacultyId + ", inquiry_id=" + inquiryId);

                // Create transaction record for the student (CHANGED action_type)
                ContentValues studentTransaction = new ContentValues();
                studentTransaction.put("user_id", studentId);
                studentTransaction.put("action_type", "Inquiry Sent"); // Changed from "Faculty Inquiry"
                studentTransaction.put("description", "Inquiry sent to " + facultyName + ": " + subject);
                studentTransaction.put("timestamp", System.currentTimeMillis());

                long studentTxId = db.insert("transactions", null, studentTransaction);
                Log.d("TRANSACTION_DEBUG", "Student transaction inserted. ID=" + studentTxId +
                        ", user_id=" + studentId + ", action_type=Inquiry Sent");

                db.setTransactionSuccessful();
                Log.d("TRANSACTION_DEBUG", "Transaction marked successful.");
                return true;
            }
            Log.w("TRANSACTION_DEBUG", "Failed to insert inquiry.");
            return false;

        } catch (Exception e) {
            e.printStackTrace();
            Log.e("TRANSACTION_DEBUG", "Exception during transaction", e);
            return false;
        } finally {
            try {
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                if (db.inTransaction()) {
                    db.endTransaction();
                    Log.d("TRANSACTION_DEBUG", "Transaction ended.");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
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