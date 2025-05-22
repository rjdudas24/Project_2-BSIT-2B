package com.example.cisync.activities;

import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;
import com.example.cisync.R;
import com.example.cisync.database.DBHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PostAccountabilityActivity extends Activity {

    private static final String TAG = "PostAccountabilityActivity";

    // UI Components
    EditText etFeeName, etAmount;
    TextView tvCurrentPosition;
    Button btnSubmit;
    ImageView btnBack;
    RadioGroup rgTargetType;
    RadioButton rbAllStudents, rbSpecificStudent;
    Spinner spTargetStudent;
    LinearLayout layoutTargetStudent;

    // Data
    DBHelper dbHelper;
    String userPosition = "";
    String userName = "";
    int studentId = -1;

    // For student targeting
    ArrayList<String> studentNames = new ArrayList<>();
    ArrayList<Integer> studentIds = new ArrayList<>();

    // Authorized positions for accountability management
    private static final List<String> AUTHORIZED_POSITIONS = Arrays.asList(
            "Treasurer",
            "Associate Treasurer",
            "Auditor"
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_accountability);

        try {
            // Initialize views
            initializeViews();

            dbHelper = new DBHelper(this);

            // Get studentId from intent
            studentId = getIntent().getIntExtra("studentId", -1);
            Log.d(TAG, "Received studentId: " + studentId);

            if (studentId == -1) {
                Toast.makeText(this, "Error: No valid user ID provided", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            // Set up back button
            btnBack.setOnClickListener(v -> finish());

            // Get current user's org position and name
            getUserDetails();

            // Check if user is authorized
            if (!isAuthorizedPosition()) {
                Toast.makeText(this, "Access denied. Only Treasurer, Associate Treasurer, and Auditor can post accountabilities.",
                        Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            // Load students for targeting
            loadStudentList();

            // Set up target type selection
            setupTargetTypeSelection();

            // Set up submit button
            btnSubmit.setOnClickListener(v -> {
                String feeName = etFeeName.getText().toString().trim();
                String amount = etAmount.getText().toString().trim();

                if (feeName.isEmpty()) {
                    Toast.makeText(this, "Please enter fee name", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (amount.isEmpty()) {
                    Toast.makeText(this, "Please enter amount", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (postAccountability(feeName, amount)) {
                    Toast.makeText(this, "Accountability posted successfully!", Toast.LENGTH_SHORT).show();
                    etFeeName.setText("");
                    etAmount.setText("");
                    finish();
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage(), e);
            Toast.makeText(this, "Error initializing Post Accountability: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initializeViews() {
        etFeeName = findViewById(R.id.etFeeName);
        etAmount = findViewById(R.id.etAmount);
        tvCurrentPosition = findViewById(R.id.tvCurrentPosition);
        btnSubmit = findViewById(R.id.btnPostAccountability);
        btnBack = findViewById(R.id.btnBack);
        rgTargetType = findViewById(R.id.rgTargetType);
        rbAllStudents = findViewById(R.id.rbAllStudents);
        rbSpecificStudent = findViewById(R.id.rbSpecificStudent);
        spTargetStudent = findViewById(R.id.spTargetStudent);
        layoutTargetStudent = findViewById(R.id.layoutTargetStudent);
    }

    private void setupTargetTypeSelection() {
        // Initially hide specific student selection
        layoutTargetStudent.setVisibility(View.GONE);

        rgTargetType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbAllStudents) {
                layoutTargetStudent.setVisibility(View.GONE);
            } else if (checkedId == R.id.rbSpecificStudent) {
                layoutTargetStudent.setVisibility(View.VISIBLE);
            }
        });

        // Default to all students
        rbAllStudents.setChecked(true);
    }

    private void loadStudentList() {
        studentNames.clear();
        studentIds.clear();

        // Add default option
        studentNames.add("Select Student");
        studentIds.add(-1);

        SQLiteDatabase db = null;
        Cursor cursor = null;

        try {
            db = dbHelper.getReadableDatabase();
            cursor = db.rawQuery(
                    "SELECT id, name FROM users WHERE role='Student' AND verified=1 AND id != ? ORDER BY name",
                    new String[]{String.valueOf(studentId)}
            );

            if (cursor.moveToFirst()) {
                do {
                    int id = cursor.getInt(0);
                    String name = cursor.getString(1);

                    studentNames.add(name);
                    studentIds.add(id);
                } while (cursor.moveToNext());
            }

            // Set up spinner
            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    this, android.R.layout.simple_spinner_item, studentNames);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spTargetStudent.setAdapter(adapter);

        } catch (Exception e) {
            Log.e(TAG, "Error loading student list: " + e.getMessage(), e);
            Toast.makeText(this, "Error loading student list", Toast.LENGTH_SHORT).show();
        } finally {
            if (cursor != null) cursor.close();
            if (db != null) db.close();
        }
    }

    private boolean postAccountability(String feeName, String amount) {
        if (studentId == -1) {
            Toast.makeText(this, "Error: Invalid user ID", Toast.LENGTH_SHORT).show();
            return false;
        }

        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();

            // Start transaction to ensure all operations succeed
            db.beginTransaction();

            try {
                // Determine target type and target students
                String targetType = "ALL";
                ArrayList<Integer> targetStudentIds = new ArrayList<>();

                if (rbSpecificStudent.isChecked()) {
                    targetType = "SPECIFIC";
                    int selectedIndex = spTargetStudent.getSelectedItemPosition();
                    if (selectedIndex > 0 && selectedIndex < studentIds.size()) {
                        targetStudentIds.add(studentIds.get(selectedIndex));
                    } else {
                        Toast.makeText(this, "Please select a target student", Toast.LENGTH_SHORT).show();
                        return false;
                    }
                } else {
                    // Get all student IDs
                    Cursor cursor = db.rawQuery(
                            "SELECT id FROM users WHERE role='Student' AND verified=1",
                            null
                    );
                    if (cursor.moveToFirst()) {
                        do {
                            targetStudentIds.add(cursor.getInt(0));
                        } while (cursor.moveToNext());
                    }
                    cursor.close();
                }

                // Insert accountability for each target student
                int successCount = 0;
                for (Integer targetId : targetStudentIds) {
                    ContentValues values = new ContentValues();
                    values.put("student_id", targetId);
                    values.put("fee_name", feeName);
                    values.put("amount", amount);
                    values.put("status", 0); // 0 = unpaid, 1 = paid
                    values.put("posted_by", studentId);
                    values.put("posted_by_name", userName);
                    values.put("posted_by_position", userPosition);
                    values.put("target_type", targetType);
                    values.put("created_at", System.currentTimeMillis());

                    long result = db.insert("accountabilities", null, values);
                    if (result != -1) {
                        successCount++;
                    }
                }

                if (successCount > 0) {
                    // Create transaction record for the org officer who posted
                    String description = "Posted accountability: " + feeName + " (₱" + amount + ")";
                    if ("SPECIFIC".equals(targetType)) {
                        String targetName = studentNames.get(spTargetStudent.getSelectedItemPosition());
                        description += " for " + targetName;
                    } else {
                        description += " for all students (" + successCount + " students)";
                    }

                    ContentValues transValues = new ContentValues();
                    transValues.put("user_id", studentId);
                    transValues.put("action_type", "Accountability Posted");
                    transValues.put("description", description);
                    transValues.put("timestamp", System.currentTimeMillis());

                    long transResult = db.insert("transactions", null, transValues);

                    if (transResult != -1) {
                        Log.d(TAG, "Transaction recorded for accountability posting: " + feeName);

                        // Commit the transaction
                        db.setTransactionSuccessful();

                        Log.d(TAG, "Accountability posted successfully for " + successCount + " students");
                        return true;
                    } else {
                        Log.e(TAG, "Failed to record transaction for accountability posting");
                        return false;
                    }
                } else {
                    Log.e(TAG, "Failed to post accountability to any students");
                    return false;
                }
            } finally {
                db.endTransaction();
                db.close();
            }

        } catch (Exception e) {
            Log.e(TAG, "Database error while posting accountability: " + e.getMessage(), e);
            Toast.makeText(this, "Database error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private void getUserDetails() {
        if (studentId == -1) {
            tvCurrentPosition.setText("Error: Invalid user");
            btnSubmit.setEnabled(false);
            return;
        }

        SQLiteDatabase db = null;
        Cursor cursor = null;

        try {
            db = dbHelper.getReadableDatabase();
            cursor = db.rawQuery(
                    "SELECT org_role, name FROM users WHERE id = ?",
                    new String[]{String.valueOf(studentId)}
            );

            if (cursor.moveToFirst()) {
                userPosition = cursor.getString(0);
                userName = cursor.getString(1);

                // Handle null or empty position
                if (userPosition == null || userPosition.isEmpty()) {
                    userPosition = "Member";
                }

                tvCurrentPosition.setText("Posting as: " + userName + " (" + userPosition + ")");
                Log.d(TAG, "User details loaded: " + userName + " - " + userPosition);
            } else {
                userPosition = "Member";
                userName = "Unknown";
                tvCurrentPosition.setText("Posting as: " + userName + " (" + userPosition + ")");
                Log.w(TAG, "No user found with ID: " + studentId);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error getting user details: " + e.getMessage(), e);
            userPosition = "Member";
            userName = "Unknown";
            tvCurrentPosition.setText("Posting as: " + userName + " (" + userPosition + ")");
            Toast.makeText(this, "Error loading user details", Toast.LENGTH_SHORT).show();
        } finally {
            if (cursor != null) cursor.close();
            if (db != null) db.close();
        }
    }

    private boolean isAuthorizedPosition() {
        return AUTHORIZED_POSITIONS.contains(userPosition);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}