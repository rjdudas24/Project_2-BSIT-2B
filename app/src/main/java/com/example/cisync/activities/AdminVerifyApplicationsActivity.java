package com.example.cisync.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import com.example.cisync.R;
import com.example.cisync.database.DBHelper;
import java.util.ArrayList;

public class AdminVerifyApplicationsActivity extends Activity {

    private ListView lvFacultyApplications;
    private TextView tvFacultyHeader;
    private ImageView btnBack;
    private DBHelper dbHelper;
    private ArrayList<String> facultyAppList = new ArrayList<>();
    private ArrayList<ApplicationData> facultyApplications = new ArrayList<>();
    private ArrayAdapter<String> facultyAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_verify_applications);

        lvFacultyApplications = findViewById(R.id.lvFacultyApplications);
        tvFacultyHeader = findViewById(R.id.tvFacultyHeader);
        btnBack = findViewById(R.id.imageView6);
        dbHelper = new DBHelper(this);

        btnBack.setOnClickListener(v -> finish());

        loadApplications();

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
        facultyAppList.clear();
        facultyApplications.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        try (Cursor cursor = db.rawQuery(
                "SELECT a.id, u.id AS user_id, u.name, u.email, u.role, a.org_name, a.status " +
                        "FROM applications a JOIN users u ON a.student_id = u.id " +
                        "WHERE a.status='Pending' AND u.role='Faculty' ORDER BY a.id DESC", null)) {

            if (cursor.moveToFirst()) {
                do {
                    int id = cursor.getInt(cursor.getColumnIndex("id"));
                    int userId = cursor.getInt(cursor.getColumnIndex("user_id"));
                    String name = cursor.getString(cursor.getColumnIndex("name"));
                    String email = cursor.getString(cursor.getColumnIndex("email"));
                    String role = cursor.getString(cursor.getColumnIndex("role"));
                    String org = cursor.getString(cursor.getColumnIndex("org_name"));
                    String status = cursor.getString(cursor.getColumnIndex("status"));

                    ApplicationData appData = new ApplicationData(id, userId, name, email, role, org, status);
                    facultyApplications.add(appData);
                    facultyAppList.add(name + " - " + email);

                } while (cursor.moveToNext());
            }
        }

        if (facultyAppList.isEmpty()) {
            facultyAppList.add("No pending faculty applications");
        }

        tvFacultyHeader.setText("Faculty Applications (" + facultyApplications.size() + ")");

        facultyAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, facultyAppList);
        lvFacultyApplications.setAdapter(facultyAdapter);
    }

    private void showFacultyApplicationDetailsDialog(ApplicationData application) {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            View dialogView = getLayoutInflater().inflate(R.layout.activity_admin_application_faculty_details, null);
            builder.setView(dialogView);

            TextView tvApplicationTitle = dialogView.findViewById(R.id.tvApplicationTitle);
            TextView tvFacultyId = dialogView.findViewById(R.id.tvFacultyID);
            TextView tvFacultyName = dialogView.findViewById(R.id.tvFacultyName);
            TextView tvFacultyEmail = dialogView.findViewById(R.id.tvFacultyEmail);
            Button btnApprove = dialogView.findViewById(R.id.btnApprove);
            Button btnReject = dialogView.findViewById(R.id.btnReject);

            tvApplicationTitle.setText("Faculty Application Details");
            tvFacultyId.setText(String.valueOf(application.getUserId()));
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
            e.printStackTrace();
            Toast.makeText(this, "Error showing faculty application details: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void approveFacultyApplication(ApplicationData application) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        try {
            db.beginTransaction();

            db.execSQL("UPDATE applications SET status='Approved' WHERE id=?", new Object[]{application.getId()});

            db.execSQL("UPDATE users SET verified=1, has_org=0, org_role='Faculty' WHERE id=?", new Object[]{application.getUserId()});

            db.execSQL("INSERT INTO transactions (user_id, action_type, description, timestamp) VALUES (?, ?, ?, ?)",
                    new Object[]{application.getUserId(), "User Verification",
                            "Approved faculty application for " + application.getName(), System.currentTimeMillis()});

            db.setTransactionSuccessful();
            Toast.makeText(this, "Faculty application approved", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error approving application: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            db.endTransaction();
            loadApplications();
        }
    }

    private void rejectApplication(ApplicationData application) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        try {
            db.beginTransaction();

            db.execSQL("UPDATE applications SET status='Rejected' WHERE id=?", new Object[]{application.getId()});

            db.execSQL("INSERT INTO transactions (user_id, action_type, description, timestamp) VALUES (?, ?, ?, ?)",
                    new Object[]{application.getUserId(), "User Verification",
                            "Rejected faculty application for " + application.getName(), System.currentTimeMillis()});

            db.setTransactionSuccessful();
            Toast.makeText(this, "Application rejected", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error rejecting application: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            db.endTransaction();
            loadApplications();
        }
    }

    private static class ApplicationData {
        private final int id;
        private final int userId;
        private final String name;
        private final String email;
        private final String role;
        private final String orgName;
        private final String status;

        public ApplicationData(int id, int userId, String name, String email, String role, String orgName, String status) {
            this.id = id;
            this.userId = userId;
            this.name = name;
            this.email = email;
            this.role = role;
            this.orgName = orgName;
            this.status = status;
        }

        public int getId() { return id; }
        public int getUserId() { return userId; }
        public String getName() { return name; }
        public String getEmail() { return email; }
        public String getRole() { return role; }
        public String getOrgName() { return orgName; }
        public String getStatus() { return status; }
    }
}