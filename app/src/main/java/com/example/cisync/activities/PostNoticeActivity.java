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
import java.util.Date;
import java.util.Locale;

public class PostNoticeActivity extends Activity {

    private static final String TAG = "PostNoticeActivity";
    EditText etNoticeTitle, etNoticeContent;
    TextView tvCurrentPosition;
    Button btnSubmit;
    ImageView btnBack;
    RadioGroup rgTargetType;
    RadioButton rbAllStudents, rbSpecificStudent;
    Spinner spTargetStudent;
    LinearLayout layoutTargetStudent;

    DBHelper dbHelper;
    String userPosition = "";
    String userName = "";
    int studentId = -1;

    // For student targeting
    ArrayList<String> studentNames = new ArrayList<>();
    ArrayList<Integer> studentIds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_notice);

        try {
            // Initialize views
            etNoticeTitle = findViewById(R.id.etNoticeTitle);
            etNoticeContent = findViewById(R.id.etNoticeContent);
            tvCurrentPosition = findViewById(R.id.tvCurrentPosition);
            btnSubmit = findViewById(R.id.btnPostNotice);
            btnBack = findViewById(R.id.btnBack);
            rgTargetType = findViewById(R.id.rgTargetType);
            rbAllStudents = findViewById(R.id.rbAllStudents);
            rbSpecificStudent = findViewById(R.id.rbSpecificStudent);
            spTargetStudent = findViewById(R.id.spTargetStudent);
            layoutTargetStudent = findViewById(R.id.layoutTargetStudent);

            dbHelper = new DBHelper(this);

            // Enhance notices table if needed (safe operation)
            dbHelper.enhanceNoticesTableIfNeeded();

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

            // Load students for targeting
            loadStudentList();

            // Set up target type selection
            setupTargetTypeSelection();

            // Set up submit button
            btnSubmit.setOnClickListener(v -> {
                String title = etNoticeTitle.getText().toString().trim();
                String content = etNoticeContent.getText().toString().trim();

                if (title.isEmpty()) {
                    Toast.makeText(this, "Please enter notice title", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (content.isEmpty()) {
                    Toast.makeText(this, "Please enter notice content", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (postNotice(title, content)) {
                    Toast.makeText(this, "Notice posted successfully!", Toast.LENGTH_SHORT).show();
                    etNoticeTitle.setText("");
                    etNoticeContent.setText("");
                    finish();
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage(), e);
            Toast.makeText(this, "Error initializing Post Notice: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
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

    private boolean postNotice(String title, String content) {
        if (studentId == -1) {
            Toast.makeText(this, "Error: Invalid user ID", Toast.LENGTH_SHORT).show();
            return false;
        }

        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();

            // Start transaction to ensure all operations succeed
            db.beginTransaction();

            try {
                String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

                // Determine target type and target student
                String targetType = "ALL";
                Integer targetStudentId = null;

                if (rbSpecificStudent.isChecked()) {
                    targetType = "SPECIFIC";
                    int selectedIndex = spTargetStudent.getSelectedItemPosition();
                    if (selectedIndex > 0 && selectedIndex < studentIds.size()) {
                        targetStudentId = studentIds.get(selectedIndex);
                    } else {
                        Toast.makeText(this, "Please select a target student", Toast.LENGTH_SHORT).show();
                        return false;
                    }
                }

                // Create content with targeting info for backward compatibility
                String formattedContent;
                if ("SPECIFIC".equals(targetType)) {
                    String targetName = studentNames.get(spTargetStudent.getSelectedItemPosition());
                    formattedContent = "[" + userPosition + " - TO: " + targetName + "] " + title + "\n\n" + content;
                } else {
                    formattedContent = "[" + userPosition + " - TO: ALL STUDENTS] " + title + "\n\n" + content;
                }

                ContentValues values = new ContentValues();
                values.put("student_id", studentId);
                values.put("content", formattedContent); // Use existing content field
                values.put("timestamp", timestamp);

                // Add new fields only if they exist (safe insertion)
                try {
                    values.put("title", title);
                    values.put("target_type", targetType);
                    values.put("target_student_id", targetStudentId);
                    values.put("posted_by_name", userName);
                    values.put("posted_by_position", userPosition);
                } catch (Exception e) {
                    // If new columns don't exist, continue with basic insertion
                    Log.w(TAG, "Using basic notice insertion: " + e.getMessage());
                }

                long noticeResult = db.insert("notices", null, values);

                if (noticeResult != -1) {
                    // Create transaction record for the student who posted the notice
                    String description = "Posted notice: " + title;
                    if ("SPECIFIC".equals(targetType)) {
                        String targetName = studentNames.get(spTargetStudent.getSelectedItemPosition());
                        description += " (targeted to: " + targetName + ")";
                    }

                    ContentValues transValues = new ContentValues();
                    transValues.put("user_id", studentId);
                    transValues.put("action_type", "Notice Posted");
                    transValues.put("description", description);
                    transValues.put("timestamp", System.currentTimeMillis());

                    long transResult = db.insert("transactions", null, transValues);

                    if (transResult != -1) {
                        Log.d(TAG, "Student transaction recorded for notice posting: " + title);

                        // Commit the transaction
                        db.setTransactionSuccessful();

                        Log.d(TAG, "Notice posted successfully with ID: " + noticeResult);
                        return true;
                    } else {
                        Log.e(TAG, "Failed to record student transaction for notice posting");
                        return false;
                    }
                } else {
                    Log.e(TAG, "Failed to insert notice into database");
                    return false;
                }
            } finally {
                db.endTransaction(); // This will rollback if setTransactionSuccessful() wasn't called
                db.close();
            }

        } catch (Exception e) {
            Log.e(TAG, "Database error while posting notice: " + e.getMessage(), e);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}