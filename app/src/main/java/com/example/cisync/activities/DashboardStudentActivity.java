package com.example.cisync.activities;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import com.example.cisync.R;
import com.example.cisync.database.DBHelper;

public class DashboardStudentActivity extends Activity {

    LinearLayout layoutInquire, layoutAccountabilities, layoutTrackDocuments, layoutPostNotice;
    TextView tvWelcome, tvPosition;
    boolean hasOrg;
    String orgPosition = "";
    int studentId = 1; // demo static ID
    DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard_student);

        hasOrg = getIntent().getBooleanExtra("hasOrg", false);

        // Get org position if available
        if (hasOrg) {
            dbHelper = new DBHelper(this);
            loadOrgPosition();
        }

        // Initialize views
        //tvWelcome = findViewById(R.id.tvWelcome);
        tvPosition = findViewById(R.id.tvPosition);
        layoutInquire = findViewById(R.id.layoutFacultyInquiry);
        layoutAccountabilities = findViewById(R.id.layoutAccountabilities);
        layoutTrackDocuments = findViewById(R.id.layoutTrackDocuments);
        layoutPostNotice = findViewById(R.id.layoutPostNotice);

        // Show organization-specific UI elements
        layoutTrackDocuments.setVisibility(hasOrg ? View.VISIBLE : View.GONE);
        layoutPostNotice.setVisibility(hasOrg ? View.VISIBLE : View.GONE);
        tvPosition.setVisibility(hasOrg ? View.VISIBLE : View.GONE);

        // Show position if student is part of an organization
        if (hasOrg && !orgPosition.isEmpty()) {
            tvPosition.setText("Position: " + orgPosition);
        }

        // Set click listeners
        layoutInquire.setOnClickListener(v -> Toast.makeText(this, "Inquire Faculty feature", Toast.LENGTH_SHORT).show());
        layoutAccountabilities.setOnClickListener(v -> startActivity(new Intent(this, ViewAccountabilitiesActivity.class)));

        // Organization-specific features
        if (hasOrg) {
            layoutTrackDocuments.setOnClickListener(v -> {
                Intent intent = new Intent(this, TrackDocumentsActivity.class);
                intent.putExtra("position", orgPosition);
                startActivity(intent);
            });

            layoutPostNotice.setOnClickListener(v -> startActivity(new Intent(this, PostNoticeActivity.class)));
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
    }
}