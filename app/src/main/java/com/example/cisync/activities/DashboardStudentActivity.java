package com.example.cisync.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import com.example.cisync.R;

public class DashboardStudentActivity extends Activity {

    LinearLayout layoutInquire, layoutAccountabilities, layoutTrackDocuments, layoutPostNotice;
    boolean hasOrg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard_student);

        hasOrg = getIntent().getBooleanExtra("hasOrg", false);

        layoutInquire = findViewById(R.id.layoutFacultyInquiry);
        layoutAccountabilities = findViewById(R.id.layoutAccountabilities);
        layoutTrackDocuments = findViewById(R.id.layoutTrackDocuments);
        layoutPostNotice = findViewById(R.id.layoutPostNotice);

        layoutTrackDocuments.setVisibility(hasOrg ? View.VISIBLE : View.GONE);
        layoutPostNotice.setVisibility(hasOrg ? View.VISIBLE : View.GONE);

        layoutInquire.setOnClickListener(v -> Toast.makeText(this, "Inquire Faculty feature", Toast.LENGTH_SHORT).show());
        layoutAccountabilities.setOnClickListener(v -> startActivity(new Intent(this, ViewAccountabilitiesActivity.class)));
        layoutTrackDocuments.setOnClickListener(v -> startActivity(new Intent(this, TrackDocumentsActivity.class)));
        layoutPostNotice.setOnClickListener(v -> startActivity(new Intent(this, PostNoticeActivity.class)));
    }
}
