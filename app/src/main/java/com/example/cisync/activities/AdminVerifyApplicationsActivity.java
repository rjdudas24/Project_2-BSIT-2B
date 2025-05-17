package com.example.cisync.activities;

import android.app.Activity;
import android.app.AlertDialog;
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

public class AdminVerifyApplicationsActivity extends Activity {

    private static final String TAG = "AdminVerifyApp";
    private ListView lvStudentApplications;
    private ListView lvFacultyApplications;
    private TextView tvStudentHeader;
    private TextView tvFacultyHeader;
    private ImageView btnBack;
    private DBHelper dbHelper;

    // Student application lists
    private ArrayList<String> studentAppList = new ArrayList<>();
    private ArrayList<ApplicationData> studentApplications = new ArrayList<>();
    private ArrayAdapter<String> studentAdapter;

    // Faculty application lists
    private ArrayList<String> facultyAppList = new ArrayList<>();
    private ArrayList<ApplicationData> facultyApplications = new ArrayList<>();
    private ArrayAdapter<String> facultyAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_admin_verify_applications);

            // Initialize UI components
            initializeUI();

            btnBack.setOnClickListener(v -> finish());

            loadApplications();

            // Set click listeners for both student and faculty application lists
            setupListeners();
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage(), e);
            Toast.makeText(this, "Error initializing application: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish(); // Close activity if it can't initialize properly
        }
    }

    private void initializeUI() {
        try {
            lvStudentApplications = findViewById(R.id.lvStudentApplications);
            lvFacultyApplications = findViewById(R.id.lvFacultyApplications);
            tvStudentHeader = findViewById(R.id.tvStudentHeader);
            tvFacultyHeader = findViewById(R.id.tvFacultyHeader);
            btnBack = findViewById(R.id.imageView6);
            dbHelper = new DBHelper(this);

            // Initialize adapters
            studentAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, studentAppList);
            lvStudentApplications.setAdapter(studentAdapter);

            facultyAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, facultyAppList);
            lvFacultyApplications.setAdapter(facultyAdapter);
        } catch (Exception e) {
            Log.e(TAG, "Error initializing UI components: " + e.getMessage(), e);
            throw e; // Rethrow to be caught by onCreate
        }
    }

    private void setupListeners() {
        lvStudentApplications.setOnItemClickListener((parent, view, position, id) -> {
            if (!studentAppList.isEmpty() && !studentAppList.get(0).equals("No pending student applications")) {
                ApplicationData application = studentApplications.get(position);
                showStudentApplicationDetailsDialog(application);
            }
        });

        lvFacultyApplications.setOnItemClickListener((parent, view, position, id) -> {
            if (!facultyAppList.isEmpty() && !facultyAppList.get(0).equals("No pending faculty applications")) {
                ApplicationData application = facultyApplications.get(position);
                showFacultyApplicationDetailsDialog(application);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadApplications();
    }

    private void loadApplications() {
        try {
            // Clear existing lists
            studentAppList.clear();
            studentApplications.clear();
            facultyAppList.clear();
            facultyApplications.clear();

            SQLiteDatabase db = dbHelper.getReadableDatabase();

            // Load student applications
            loadStudentApplications(db);

            // Load faculty applications
            loadFacultyApplications(db);

            // Update headers with application counts
            tvStudentHeader.setText("Student Applications (" + studentApplications.size() + ")");
            tvFacultyHeader.setText("Faculty Applications (" + facultyApplications.size() + ")");

            // Update adapters
            studentAdapter.notifyDataSetChanged();
            facultyAdapter.notifyDataSetChanged();
        } catch (Exception e) {
            Log.e(TAG, "Error loading applications: " + e.getMessage(), e);
            Toast.makeText(this, "Error loading applications: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void loadStudentApplications(SQLiteDatabase db) {
        // Query to retrieve student applications with status 'Pending'
        String studentQuery = "SELECT a.id, u.id AS user_id, u.name, u.email, u.role, u.has_org, u.org_role, " +
                "a.org_name, a.status, a.created_at " +
                "FROM applications a JOIN users u ON a.student_id = u.id " +
                "WHERE a.status='Pending' AND u.role='Student' AND u.verified=0 " +
                "ORDER BY a.created_at DESC";

        try (Cursor cursor = db.rawQuery(studentQuery, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    // Get column indices safely
                    int idIdx = cursor.getColumnIndex("id");
                    int userIdIdx = cursor.getColumnIndex("user_id");
                    int nameIdx = cursor.getColumnIndex("name");
                    int emailIdx = cursor.getColumnIndex("email");
                    int roleIdx = cursor.getColumnIndex("role");
                    int hasOrgIdx = cursor.getColumnIndex("has_org");
                    int orgRoleIdx = cursor.getColumnIndex("org_role");
                    int orgNameIdx = cursor.getColumnIndex("org_name");
                    int statusIdx = cursor.getColumnIndex("status");
                    int createdAtIdx = cursor.getColumnIndex("created_at");

                    // Safely get data from cursor
                    int id = idIdx != -1 ? cursor.getInt(idIdx) : -1;
                    int userId = userIdIdx != -1 ? cursor.getInt(userIdIdx) : -1;
                    String name = nameIdx != -1 ? cursor.getString(nameIdx) : "Unknown";
                    String email = emailIdx != -1 ? cursor.getString(emailIdx) : "No email";
                    String role = roleIdx != -1 ? cursor.getString(roleIdx) : "Student";
                    int hasOrg = hasOrgIdx != -1 ? cursor.getInt(hasOrgIdx) : 0;
                    String orgRole = orgRoleIdx != -1 ? cursor.getString(orgRoleIdx) : "";
                    String orgName = orgNameIdx != -1 ? cursor.getString(orgNameIdx) : "";
                    String status = statusIdx != -1 ? cursor.getString(statusIdx) : "Pending";
                    long createdAt = createdAtIdx != -1 ? cursor.getLong(createdAtIdx) : 0;

                    // Get ID number from separate query
                    String idNumber = getIdNumber(db, userId);

                    ApplicationData appData = new ApplicationData(id, userId, name, email, role, orgName, status);
                    appData.setHasOrg(hasOrg);
                    appData.setOrgRole(orgRole);
                    appData.setIdNumber(idNumber);
                    appData.setTimestamp(createdAt);

                    studentApplications.add(appData);

                    // Format the registration date
                    String dateInfo = formatDate(createdAt);

                    // Add indicator if student is part of an organization
                    String displayText = name + " - " + email;
                    if (hasOrg == 1) {
                        displayText += " (Org: " + orgRole + ")";
                    }
                    displayText += "\n" + dateInfo;
                    studentAppList.add(displayText);

                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading student applications: " + e.getMessage(), e);
        }

        if (studentAppList.isEmpty()) {
            studentAppList.add("No pending student applications");
        }
    }

    private void loadFacultyApplications(SQLiteDatabase db) {
        // Query to retrieve faculty applications with status 'Pending'
        String facultyQuery = "SELECT a.id, u.id AS user_id, u.name, u.email, u.role, " +
                "a.org_name, a.status, a.created_at " +
                "FROM applications a JOIN users u ON a.student_id = u.id " +
                "WHERE a.status='Pending' AND u.role='Faculty' AND u.verified=0 " +
                "ORDER BY a.created_at DESC";

        try (Cursor cursor = db.rawQuery(facultyQuery, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    // Get column indices safely
                    int idIdx = cursor.getColumnIndex("id");
                    int userIdIdx = cursor.getColumnIndex("user_id");
                    int nameIdx = cursor.getColumnIndex("name");
                    int emailIdx = cursor.getColumnIndex("email");
                    int roleIdx = cursor.getColumnIndex("role");
                    int orgNameIdx = cursor.getColumnIndex("org_name");
                    int statusIdx = cursor.getColumnIndex("status");
                    int createdAtIdx = cursor.getColumnIndex("created_at");

                    // Safely get data from cursor
                    int id = idIdx != -1 ? cursor.getInt(idIdx) : -1;
                    int userId = userIdIdx != -1 ? cursor.getInt(userIdIdx) : -1;
                    String name = nameIdx != -1 ? cursor.getString(nameIdx) : "Unknown";
                    String email = emailIdx != -1 ? cursor.getString(emailIdx) : "No email";
                    String role = roleIdx != -1 ? cursor.getString(roleIdx) : "Faculty";
                    String orgName = orgNameIdx != -1 ? cursor.getString(orgNameIdx) : "";
                    String status = statusIdx != -1 ? cursor.getString(statusIdx) : "Pending";
                    long createdAt = createdAtIdx != -1 ? cursor.getLong(createdAtIdx) : 0;

                    // Get ID number from separate query
                    String idNumber = getIdNumber(db, userId);

                    ApplicationData appData = new ApplicationData(id, userId, name, email, role, orgName, status);
                    appData.setIdNumber(idNumber);
                    appData.setTimestamp(createdAt);

                    facultyApplications.add(appData);

                    // Format the registration date
                    String dateInfo = formatDate(createdAt);
                    facultyAppList.add(name + " - " + email + "\n" + dateInfo);

                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading faculty applications: " + e.getMessage(), e);
        }

        if (facultyAppList.isEmpty()) {
            facultyAppList.add("No pending faculty applications");
        }
    }

    // Helper method to get ID number from user ID
    private String getIdNumber(SQLiteDatabase db, int userId) {
        String idNumber = "";
        try {
            Cursor cursor = db.rawQuery(
                    "SELECT id_number FROM users WHERE id=?",
                    new String[]{String.valueOf(userId)}
            );

            if (cursor != null && cursor.moveToFirst()) {
                int idNumberIdx = cursor.getColumnIndex("id_number");
                if (idNumberIdx != -1) {
                    idNumber = cursor.getString(idNumberIdx);
                }
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving ID number: " + e.getMessage(), e);
        }
        return idNumber != null ? idNumber : "";
    }

    private String formatDate(long timestamp) {
        if (timestamp == 0) {
            return "Date unknown";
        }
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
        return "Registered: " + sdf.format(new Date(timestamp));
    }

    private void showStudentApplicationDetailsDialog(ApplicationData application) {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            View dialogView = getLayoutInflater().inflate(R.layout.activity_admin_application_details, null);
            builder.setView(dialogView);

            // Find and set up views
            TextView tvApplicationTitle = dialogView.findViewById(R.id.tvApplicationTitle);
            TextView tvStudentId = dialogView.findViewById(R.id.tvStudentID);
            TextView tvStudentName = dialogView.findViewById(R.id.tvStudentName);
            TextView tvStudentEmail = dialogView.findViewById(R.id.tvStudentEmail);
            TextView tvOrgName = dialogView.findViewById(R.id.tvOrgName);
            Button btnApprove = dialogView.findViewById(R.id.btnApprove);
            Button btnReject = dialogView.findViewById(R.id.btnReject);

            // Set text values
            tvApplicationTitle.setText("Student Application Details");
            tvStudentId.setText(application.getIdNumber() != null && !application.getIdNumber().isEmpty() ?
                    application.getIdNumber() : "Not provided");
            tvStudentName.setText(application.getName());
            tvStudentEmail.setText(application.getEmail());

            // Show organization information if applicable
            if (application.getHasOrg() == 1) {
                tvOrgName.setText(application.getOrgName() + " (" + application.getOrgRole() + ")");
            } else {
                tvOrgName.setText("Not affiliated with any organization");
            }

            AlertDialog dialog = builder.create();
            dialog.show();

            btnApprove.setOnClickListener(v -> {
                approveStudentApplication(application);
                dialog.dismiss();
            });

            btnReject.setOnClickListener(v -> {
                rejectApplication(application);
                dialog.dismiss();
            });

        } catch (Exception e) {
            Log.e(TAG, "Error showing student application details: " + e.getMessage(), e);
            Toast.makeText(this, "Error showing student application details: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showFacultyApplicationDetailsDialog(ApplicationData application) {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            View dialogView = getLayoutInflater().inflate(R.layout.activity_admin_application_faculty_details, null);
            builder.setView(dialogView);

            // Find and set up views
            TextView tvApplicationTitle = dialogView.findViewById(R.id.tvApplicationTitle);
            TextView tvFacultyId = dialogView.findViewById(R.id.tvFacultyID);
            TextView tvFacultyName = dialogView.findViewById(R.id.tvFacultyName);
            TextView tvFacultyEmail = dialogView.findViewById(R.id.tvFacultyEmail);
            Button btnApprove = dialogView.findViewById(R.id.btnApprove);
            Button btnReject = dialogView.findViewById(R.id.btnReject);

            // Set text values
            tvApplicationTitle.setText("Faculty Application Details");
            tvFacultyId.setText(application.getIdNumber() != null && !application.getIdNumber().isEmpty() ?
                    application.getIdNumber() : "Not provided");
            tvFacultyName.setText(application.getName());
            tvFacultyEmail.setText(application.getEmail());

            AlertDialog dialog = builder.create();
            dialog.show();

            btnApprove.setOnClickListener(v -> {
                approveFacultyApplication(application);
                dialog.dismiss();
            });

            btnReject.setOnClickListener(v -> {
                rejectApplication(application);
                dialog.dismiss();
            });

        } catch (Exception e) {
            Log.e(TAG, "Error showing faculty application details: " + e.getMessage(), e);
            Toast.makeText(this, "Error showing faculty application details: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void approveStudentApplication(ApplicationData application) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        try {
            db.beginTransaction();

            // Update application status
            ContentValues appValues = new ContentValues();
            appValues.put("status", "Approved");
            db.update("applications", appValues, "id=?",
                    new String[]{String.valueOf(application.getId())});

            // Set verified flag and update organization info
            ContentValues userValues = new ContentValues();
            userValues.put("verified", 1);

            if (application.getHasOrg() == 1) {
                userValues.put("has_org", 1);
                userValues.put("org_role", application.getOrgRole());
            } else {
                userValues.put("has_org", 0);
            }

            db.update("users", userValues, "id=?",
                    new String[]{String.valueOf(application.getUserId())});

            // Log the transaction
            ContentValues transValues = new ContentValues();
            transValues.put("user_id", application.getUserId());
            transValues.put("action_type", "User Verification");
            transValues.put("description", "Approved student application for " + application.getName());
            transValues.put("timestamp", System.currentTimeMillis());
            db.insert("transactions", null, transValues);

            db.setTransactionSuccessful();
            Toast.makeText(this, "Student application approved", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Error approving student application: " + e.getMessage(), e);
            Toast.makeText(this, "Error approving application: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            if (db.inTransaction()) {
                db.endTransaction();
            }
            loadApplications();
        }
    }

    private void approveFacultyApplication(ApplicationData application) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        try {
            db.beginTransaction();

            // Update application status
            ContentValues appValues = new ContentValues();
            appValues.put("status", "Approved");
            db.update("applications", appValues, "id=?",
                    new String[]{String.valueOf(application.getId())});

            // Update faculty user status
            ContentValues userValues = new ContentValues();
            userValues.put("verified", 1);
            userValues.put("has_org", 0);
            userValues.put("org_role", "Faculty");
            db.update("users", userValues, "id=?",
                    new String[]{String.valueOf(application.getUserId())});

            // Log the transaction
            ContentValues transValues = new ContentValues();
            transValues.put("user_id", application.getUserId());
            transValues.put("action_type", "User Verification");
            transValues.put("description", "Approved faculty application for " + application.getName());
            transValues.put("timestamp", System.currentTimeMillis());
            db.insert("transactions", null, transValues);

            db.setTransactionSuccessful();
            Toast.makeText(this, "Faculty application approved", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Error approving faculty application: " + e.getMessage(), e);
            Toast.makeText(this, "Error approving application: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            if (db.inTransaction()) {
                db.endTransaction();
            }
            loadApplications();
        }
    }

    private void rejectApplication(ApplicationData application) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        try {
            db.beginTransaction();

            // Update application status
            ContentValues appValues = new ContentValues();
            appValues.put("status", "Rejected");
            db.update("applications", appValues, "id=?",
                    new String[]{String.valueOf(application.getId())});

            // Log the transaction
            ContentValues transValues = new ContentValues();
            transValues.put("user_id", application.getUserId());
            transValues.put("action_type", "User Verification");
            transValues.put("description", "Rejected " + application.getRole().toLowerCase() + " application for " +
                    application.getName());
            transValues.put("timestamp", System.currentTimeMillis());
            db.insert("transactions", null, transValues);

            db.setTransactionSuccessful();
            Toast.makeText(this, "Application rejected", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Error rejecting application: " + e.getMessage(), e);
            Toast.makeText(this, "Error rejecting application: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            if (db.inTransaction()) {
                db.endTransaction();
            }
            loadApplications();
        }
    }

    // Static inner class to hold application data
    private static class ApplicationData {
        private final int id;
        private final int userId;
        private final String name;
        private final String email;
        private final String role;
        private final String orgName;
        private final String status;
        private int hasOrg = 0;
        private String orgRole = "";
        private String idNumber = "";
        private long timestamp = 0;

        public ApplicationData(int id, int userId, String name, String email, String role, String orgName, String status) {
            this.id = id;
            this.userId = userId;
            this.name = name != null ? name : "Unknown";
            this.email = email != null ? email : "";
            this.role = role != null ? role : "";
            this.orgName = orgName != null ? orgName : "";
            this.status = status != null ? status : "Pending";
        }

        public int getId() { return id; }
        public int getUserId() { return userId; }
        public String getName() { return name; }
        public String getEmail() { return email; }
        public String getRole() { return role; }
        public String getOrgName() { return orgName; }
        public String getStatus() { return status; }

        public int getHasOrg() { return hasOrg; }
        public void setHasOrg(int hasOrg) { this.hasOrg = hasOrg; }

        public String getOrgRole() { return orgRole; }
        public void setOrgRole(String orgRole) { this.orgRole = orgRole != null ? orgRole : ""; }

        public String getIdNumber() { return idNumber; }
        public void setIdNumber(String idNumber) { this.idNumber = idNumber != null ? idNumber : ""; }

        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }
}