package com.example.cisync.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.widget.*;
import com.example.cisync.R;
import com.example.cisync.database.DBHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ManageAccountabilitiesActivity extends Activity {

    private static final String TAG = "ManageAccountabilities";

    ListView lvAccountabilities;
    TextView tvNoAccountabilities;
    Spinner spinnerStudentFilter, spinnerStatusFilter;
    Button btnApplyFilter, btnClearFilter;
    ImageView btnBack;

    DBHelper dbHelper;
    ArrayList<String> accountabilityList = new ArrayList<>();
    ArrayList<AccountabilityData> accountabilityDataList = new ArrayList<>();
    ArrayAdapter<String> adapter;
    int studentId = -1;
    String userPosition = "";

    // Filter options
    ArrayList<String> studentFilterNames = new ArrayList<>();
    ArrayList<Integer> studentFilterIds = new ArrayList<>();

    // Authorized positions for accountability management
    private static final List<String> AUTHORIZED_POSITIONS = Arrays.asList(
            "Treasurer",
            "Associate Treasurer",
            "Auditor"
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_accountabilities);

        // Get student ID from intent
        studentId = getIntent().getIntExtra("studentId", -1);

        if (studentId == -1) {
            Log.e(TAG, "No valid student ID received");
            Toast.makeText(this, "Error: No valid student ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Log.d(TAG, "Student ID: " + studentId);

        // Initialize views and setup
        initializeViews();
        getUserPosition();

        if (!isAuthorizedPosition()) {
            Toast.makeText(this, "Access denied. Only Treasurer, Associate Treasurer, and Auditor can manage accountabilities.",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        setupFilters();
        setupClickListeners();
        loadAccountabilities(null, null);
    }

    private void initializeViews() {
        try {
            lvAccountabilities = findViewById(R.id.lvAccountabilities);
            tvNoAccountabilities = findViewById(R.id.tvNoAccountabilities);
            spinnerStudentFilter = findViewById(R.id.spinnerStudentFilter);
            spinnerStatusFilter = findViewById(R.id.spinnerStatusFilter);
            btnApplyFilter = findViewById(R.id.btnApplyFilter);
            btnClearFilter = findViewById(R.id.btnClearFilter);
            btnBack = findViewById(R.id.btnBack);
            dbHelper = new DBHelper(this);
        } catch (Exception e) {
            Log.e(TAG, "Error initializing views: " + e.getMessage(), e);
            Toast.makeText(this, "Error initializing views", Toast.LENGTH_SHORT).show();
        }
    }

    private void getUserPosition() {
        SQLiteDatabase db = null;
        Cursor cursor = null;

        try {
            db = dbHelper.getReadableDatabase();
            cursor = db.rawQuery(
                    "SELECT org_role FROM users WHERE id = ?",
                    new String[]{String.valueOf(studentId)}
            );

            if (cursor.moveToFirst()) {
                userPosition = cursor.getString(0);
                if (userPosition == null || userPosition.isEmpty()) {
                    userPosition = "Member";
                }
                Log.d(TAG, "User position: " + userPosition);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error getting user position: " + e.getMessage(), e);
            userPosition = "Member";
        } finally {
            if (cursor != null) cursor.close();
            if (db != null) db.close();
        }
    }

    private boolean isAuthorizedPosition() {
        return AUTHORIZED_POSITIONS.contains(userPosition);
    }

    private void setupFilters() {
        try {
            // Setup student filter
            studentFilterNames.clear();
            studentFilterIds.clear();
            studentFilterNames.add("All Students");
            studentFilterIds.add(-1);

            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = db.rawQuery(
                    "SELECT DISTINCT u.id, u.name FROM users u " +
                            "INNER JOIN accountabilities a ON u.id = a.student_id " +
                            "WHERE u.role='Student' AND u.verified=1 ORDER BY u.name",
                    null
            );

            if (cursor.moveToFirst()) {
                do {
                    int id = cursor.getInt(0);
                    String name = cursor.getString(1);
                    studentFilterNames.add(name);
                    studentFilterIds.add(id);
                } while (cursor.moveToNext());
            }
            cursor.close();

            ArrayAdapter<String> studentAdapter = new ArrayAdapter<>(
                    this, android.R.layout.simple_spinner_item, studentFilterNames);
            studentAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerStudentFilter.setAdapter(studentAdapter);

            // Setup status filter
            ArrayList<String> statusOptions = new ArrayList<>();
            statusOptions.add("All Status");
            statusOptions.add("Paid");
            statusOptions.add("Unpaid");

            ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(
                    this, android.R.layout.simple_spinner_item, statusOptions);
            statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerStatusFilter.setAdapter(statusAdapter);

        } catch (Exception e) {
            Log.e(TAG, "Error setting up filters: " + e.getMessage(), e);
        }
    }

    private void setupClickListeners() {
        try {
            // Back button
            btnBack.setOnClickListener(v -> finish());

            // Apply filter button
            btnApplyFilter.setOnClickListener(v -> {
                String studentFilter = null;
                String statusFilter = null;

                int studentIndex = spinnerStudentFilter.getSelectedItemPosition();
                if (studentIndex > 0) {
                    studentFilter = String.valueOf(studentFilterIds.get(studentIndex));
                }

                int statusIndex = spinnerStatusFilter.getSelectedItemPosition();
                if (statusIndex == 1) {
                    statusFilter = "1"; // Paid
                } else if (statusIndex == 2) {
                    statusFilter = "0"; // Unpaid
                }

                loadAccountabilities(studentFilter, statusFilter);
            });

            // Clear filter button
            btnClearFilter.setOnClickListener(v -> {
                spinnerStudentFilter.setSelection(0);
                spinnerStatusFilter.setSelection(0);
                loadAccountabilities(null, null);
            });

            // List item click listener for status management
            lvAccountabilities.setOnItemClickListener((parent, view, position, id) -> {
                if (position < accountabilityDataList.size()) {
                    AccountabilityData accountability = accountabilityDataList.get(position);
                    showAccountabilityOptionsDialog(accountability);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error setting up click listeners: " + e.getMessage(), e);
        }
    }

    private void loadAccountabilities(String studentFilter, String statusFilter) {
        accountabilityList.clear();
        accountabilityDataList.clear();

        SQLiteDatabase db = dbHelper.getReadableDatabase();

        try {
            StringBuilder query = new StringBuilder();
            query.append("SELECT a.id, a.student_id, a.fee_name, a.amount, a.status, ");
            query.append("a.posted_by_name, a.posted_by_position, a.created_at, u.name as student_name ");
            query.append("FROM accountabilities a ");
            query.append("INNER JOIN users u ON a.student_id = u.id ");
            query.append("WHERE 1=1 ");

            ArrayList<String> selectionArgs = new ArrayList<>();

            if (studentFilter != null) {
                query.append("AND a.student_id = ? ");
                selectionArgs.add(studentFilter);
            }

            if (statusFilter != null) {
                query.append("AND a.status = ? ");
                selectionArgs.add(statusFilter);
            }

            query.append("ORDER BY a.created_at DESC");

            Cursor cursor = db.rawQuery(query.toString(),
                    selectionArgs.toArray(new String[0]));

            if (cursor.moveToFirst() && cursor.getCount() > 0) {
                tvNoAccountabilities.setVisibility(ListView.GONE);
                lvAccountabilities.setVisibility(ListView.VISIBLE);

                do {
                    int id = cursor.getInt(0);
                    int studentIdTarget = cursor.getInt(1);
                    String feeName = cursor.getString(2);
                    String amount = cursor.getString(3);
                    int status = cursor.getInt(4);
                    String postedByName = cursor.getString(5);
                    String postedByPosition = cursor.getString(6);
                    long createdAt = cursor.getLong(7);
                    String studentName = cursor.getString(8);

                    AccountabilityData data = new AccountabilityData(
                            id, studentIdTarget, feeName, amount, status == 1,
                            postedByName, postedByPosition, createdAt, studentName);
                    accountabilityDataList.add(data);

                    // Format for display
                    String statusText = status == 1 ? "✅ PAID" : "❌ UNPAID";
                    String formattedDate = formatTimestamp(createdAt);

                    String displayText = feeName + " - ₱" + amount + "\n" +
                            "Student: " + studentName + "\n" +
                            "Status: " + statusText + "\n" +
                            "Posted by: " + postedByName + " (" + postedByPosition + ")\n" +
                            "Date: " + formattedDate;

                    accountabilityList.add(displayText);
                } while (cursor.moveToNext());

            } else {
                tvNoAccountabilities.setVisibility(ListView.VISIBLE);
                lvAccountabilities.setVisibility(ListView.GONE);
                tvNoAccountabilities.setText("No accountabilities found");
            }

            cursor.close();

            adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, accountabilityList);
            lvAccountabilities.setAdapter(adapter);

        } catch (Exception e) {
            Log.e(TAG, "Error loading accountabilities: " + e.getMessage(), e);
            Toast.makeText(this, "Error loading accountabilities: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            tvNoAccountabilities.setVisibility(ListView.VISIBLE);
            tvNoAccountabilities.setText("Error loading accountabilities");
            lvAccountabilities.setVisibility(ListView.GONE);
        }
    }

    private void showAccountabilityOptionsDialog(AccountabilityData accountability) {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Manage Accountability");

            String currentStatus = accountability.isPaid() ? "Paid" : "Unpaid";
            String newStatus = accountability.isPaid() ? "Unpaid" : "Paid";

            String[] options = {
                    "Change Status to " + newStatus,
                    "View Details",
                    "Delete Accountability"
            };

            builder.setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0:
                        changeAccountabilityStatus(accountability);
                        break;
                    case 1:
                        showAccountabilityDetails(accountability);
                        break;
                    case 2:
                        showDeleteConfirmation(accountability);
                        break;
                }
            });

            builder.setNegativeButton("Cancel", null);
            builder.show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing options dialog: " + e.getMessage(), e);
        }
    }

    private void changeAccountabilityStatus(AccountabilityData accountability) {
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            db.beginTransaction();

            try {
                int newStatus = accountability.isPaid() ? 0 : 1;
                String statusText = newStatus == 1 ? "paid" : "unpaid";

                ContentValues values = new ContentValues();
                values.put("status", newStatus);

                int result = db.update("accountabilities", values, "id=?",
                        new String[]{String.valueOf(accountability.getId())});

                if (result > 0) {
                    // Record transaction for the org officer
                    ContentValues transValues = new ContentValues();
                    transValues.put("user_id", studentId);
                    transValues.put("action_type", "Accountability Status Update");
                    transValues.put("description", "Changed " + accountability.getFeeName() +
                            " status to " + statusText + " for " + accountability.getStudentName());
                    transValues.put("timestamp", System.currentTimeMillis());

                    db.insert("transactions", null, transValues);
                    db.setTransactionSuccessful();

                    Toast.makeText(this, "Status updated to " + statusText, Toast.LENGTH_SHORT).show();
                    loadAccountabilities(null, null); // Refresh list
                } else {
                    Toast.makeText(this, "Failed to update status", Toast.LENGTH_SHORT).show();
                }
            } finally {
                db.endTransaction();
                db.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error changing status: " + e.getMessage(), e);
            Toast.makeText(this, "Error updating status", Toast.LENGTH_SHORT).show();
        }
    }

    private void showAccountabilityDetails(AccountabilityData accountability) {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Accountability Details");

            String message = "Fee: " + accountability.getFeeName() + "\n" +
                    "Amount: ₱" + accountability.getAmount() + "\n" +
                    "Student: " + accountability.getStudentName() + "\n" +
                    "Status: " + (accountability.isPaid() ? "Paid" : "Unpaid") + "\n" +
                    "Posted by: " + accountability.getPostedByName() +
                    " (" + accountability.getPostedByPosition() + ")\n" +
                    "Date Posted: " + formatTimestamp(accountability.getCreatedAt()) + "\n" +
                    "ID: " + accountability.getId();

            builder.setMessage(message);
            builder.setPositiveButton("Close", null);
            builder.show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing details: " + e.getMessage(), e);
        }
    }

    private void showDeleteConfirmation(AccountabilityData accountability) {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Delete Accountability");
            builder.setMessage("Are you sure you want to delete this accountability?\n\n" +
                    accountability.getFeeName() + " - ₱" + accountability.getAmount() +
                    "\nFor: " + accountability.getStudentName());

            builder.setPositiveButton("Delete", (dialog, which) -> {
                deleteAccountability(accountability);
            });

            builder.setNegativeButton("Cancel", null);
            builder.show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing delete confirmation: " + e.getMessage(), e);
        }
    }

    private void deleteAccountability(AccountabilityData accountability) {
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            db.beginTransaction();

            try {
                int result = db.delete("accountabilities", "id=?",
                        new String[]{String.valueOf(accountability.getId())});

                if (result > 0) {
                    // Record transaction for the org officer
                    ContentValues transValues = new ContentValues();
                    transValues.put("user_id", studentId);
                    transValues.put("action_type", "Accountability Deleted");
                    transValues.put("description", "Deleted accountability: " + accountability.getFeeName() +
                            " (₱" + accountability.getAmount() + ") for " + accountability.getStudentName());
                    transValues.put("timestamp", System.currentTimeMillis());

                    db.insert("transactions", null, transValues);
                    db.setTransactionSuccessful();

                    Toast.makeText(this, "Accountability deleted successfully", Toast.LENGTH_SHORT).show();
                    loadAccountabilities(null, null); // Refresh list
                } else {
                    Toast.makeText(this, "Failed to delete accountability", Toast.LENGTH_SHORT).show();
                }
            } finally {
                db.endTransaction();
                db.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error deleting accountability: " + e.getMessage(), e);
            Toast.makeText(this, "Error deleting accountability", Toast.LENGTH_SHORT).show();
        }
    }

    private String formatTimestamp(long timestamp) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        } catch (Exception e) {
            return "Unknown date";
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }

    // Data class for accountability information
    private static class AccountabilityData {
        private final int id;
        private final int studentId;
        private final String feeName;
        private final String amount;
        private final boolean isPaid;
        private final String postedByName;
        private final String postedByPosition;
        private final long createdAt;
        private final String studentName;

        public AccountabilityData(int id, int studentId, String feeName, String amount,
                                  boolean isPaid, String postedByName, String postedByPosition,
                                  long createdAt, String studentName) {
            this.id = id;
            this.studentId = studentId;
            this.feeName = feeName;
            this.amount = amount;
            this.isPaid = isPaid;
            this.postedByName = postedByName;
            this.postedByPosition = postedByPosition;
            this.createdAt = createdAt;
            this.studentName = studentName;
        }

        public int getId() { return id; }
        public int getStudentId() { return studentId; }
        public String getFeeName() { return feeName; }
        public String getAmount() { return amount; }
        public boolean isPaid() { return isPaid; }
        public String getPostedByName() { return postedByName; }
        public String getPostedByPosition() { return postedByPosition; }
        public long getCreatedAt() { return createdAt; }
        public String getStudentName() { return studentName; }
    }
}