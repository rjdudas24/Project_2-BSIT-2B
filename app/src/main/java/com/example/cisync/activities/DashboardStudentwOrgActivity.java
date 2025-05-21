package com.example.cisync.activities;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;
import com.example.cisync.R;
import com.example.cisync.database.DBHelper;

public class DashboardStudentwOrgActivity extends Activity {

    LinearLayout layoutInquire, layoutAccountabilities, layoutTrackDocuments, layoutPostNotice;
    TextView tvWelcome, tvOrganizationValue, tvPositionValue;
    boolean hasOrg;
    String orgPosition = "";
    int studentId; // No longer a static value
    DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard_student_org);

        // Get data from intent
        hasOrg = getIntent().getBooleanExtra("hasOrg", false);
        studentId = getIntent().getIntExtra("studentId", -1); // Get actual studentId

        // Initialize dbHelper
        dbHelper = new DBHelper(this);

        // Initialize views
        //tvWelcome = findViewById(R.id.tvWelcome);
        tvOrganizationValue = findViewById(R.id.tvOrganizationValue);
        tvPositionValue = findViewById(R.id.tvPositionValue);
        layoutInquire = findViewById(R.id.layoutFacultyInquiry);
        layoutAccountabilities = findViewById(R.id.layoutAccountabilities);
        layoutTrackDocuments = findViewById(R.id.layoutTrackDocuments);
        layoutPostNotice = findViewById(R.id.layoutPostNotice);

        // Get org position if available
        if (hasOrg && studentId != -1) {
            loadOrgPosition();
        }

        // Show organization-specific UI elements
        layoutTrackDocuments.setVisibility(hasOrg ? View.VISIBLE : View.GONE);
        layoutPostNotice.setVisibility(hasOrg ? View.VISIBLE : View.GONE);

        // Show position if student is part of an organization
        if (hasOrg && !orgPosition.isEmpty()) {
            tvPositionValue.setText(orgPosition);
        }

        // Set click listeners
        layoutInquire.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DashboardStudentwOrgActivity.this, FacultyInquiryActivity.class);
                intent.putExtra("studentId", studentId);
                startActivity(intent);
            }
        });

        layoutAccountabilities.setOnClickListener(v -> {
            Intent intent = new Intent(this, ViewAccountabilitiesActivity.class);
            intent.putExtra("studentId", studentId); // Pass the student ID
            startActivity(intent);
        });

        // Organization-specific features
        if (hasOrg) {
            layoutTrackDocuments.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(this, TrackDocumentsActivity.class);
                    intent.putExtra("position", orgPosition);
                    intent.putExtra("studentId", studentId);
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e("DashboardStudentOrg", "Error launching Track Documents: " + e.getMessage(), e);
                    Toast.makeText(this, "Error opening document tracking: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void loadOrgPosition() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT org_role FROM users WHERE id = ?",
                new String[]{String.valueOf(studentId)}
        );

        if (cursor.moveToFirst()) {
            orgPosition = cursor.getString(0);
        }

        cursor.close();

        // Now also load organization name and set it in the UI
        cursor = db.rawQuery(
                "SELECT name FROM users WHERE id = ?",
                new String[]{String.valueOf(studentId)}
        );

        if (cursor.moveToFirst()) {
            String name = cursor.getString(0);
            // Set organization name
            if (tvOrganizationValue != null) {
                tvOrganizationValue.setText("CSCo"); // You might want to replace this with organization name from DB
            }
        }

        cursor.close();
    }
}