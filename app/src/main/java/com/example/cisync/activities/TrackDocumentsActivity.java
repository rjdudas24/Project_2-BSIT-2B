package com.example.cisync.activities;

import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import com.example.cisync.R;
import com.example.cisync.database.DBHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class TrackDocumentsActivity extends Activity {

    TextView tvPosition;
    ListView lvDocuments;
    EditText etDocumentName, etDocumentDesc;
    Button btnAddDocument;
    DBHelper dbHelper;
    ArrayList<String> documentsList = new ArrayList<>();
    ArrayAdapter<String> adapter;
    String userPosition;
    int studentId = -1;
    boolean isLeadershipPosition = false;

    // Define which positions are considered leadership positions
    private static final String[] LEADERSHIP_POSITIONS = {
            "Chairperson",
            "Vice-Chairperson (Internal)",
            "Vice-Chairperson (External)",
            "Secretary",
            "Associate Secretary",
            "Treasurer",
            "Associate Treasurer",
            "Auditor"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_documents);

        // Get data from intent
        userPosition = getIntent().getStringExtra("position");
        studentId = getIntent().getIntExtra("studentId", -1);

        if (userPosition == null || userPosition.isEmpty()) {
            userPosition = "Member"; // Default
        }

        // Check if user has a leadership position
        checkLeadershipAccess();

        tvPosition = findViewById(R.id.tvUserPosition);
        lvDocuments = findViewById(R.id.lvDocuments);
        etDocumentName = findViewById(R.id.etDocumentName);
        etDocumentDesc = findViewById(R.id.etDocumentDesc);
        btnAddDocument = findViewById(R.id.btnAddDocument);

        dbHelper = new DBHelper(this);

        // Set position text
        tvPosition.setText("Position: " + userPosition);

        // Show/hide document creation UI based on position
        View layoutAddDocument = findViewById(R.id.layoutAddDocument);
        layoutAddDocument.setVisibility(isLeadershipPosition ? View.VISIBLE : View.GONE);

        btnAddDocument.setOnClickListener(v -> addNewDocument());

        loadDocuments();
    }

    private void checkLeadershipAccess() {
        for (String position : LEADERSHIP_POSITIONS) {
            if (position.equals(userPosition)) {
                isLeadershipPosition = true;
                break;
            }
        }
    }

    private void loadDocuments() {
        documentsList.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT name, description, status, created_by, timestamp FROM documents ORDER BY timestamp DESC", null);

        if (cursor.moveToFirst()) {
            do {
                String name = cursor.getString(0);
                String desc = cursor.getString(1);
                String status = cursor.getString(2);
                String creator = cursor.getString(3);
                String time = cursor.getString(4);

                documentsList.add(name + "\n" + desc + "\nStatus: " + status +
                        "\nCreated by: " + creator + " on " + time);

            } while (cursor.moveToNext());
        } else {
            // No documents found
            documentsList.add("No documents available");
        }

        cursor.close();

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, documentsList);
        lvDocuments.setAdapter(adapter);
    }

    private void addNewDocument() {
        String name = etDocumentName.getText().toString().trim();
        String desc = etDocumentDesc.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(this, "Document name is required", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if we have a valid student ID
        if (studentId == -1) {
            Toast.makeText(this, "Error: Invalid user ID", Toast.LENGTH_SHORT).show();
            return;
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", name);
        cv.put("description", desc);
        cv.put("status", "Pending");
        cv.put("created_by", userPosition);
        cv.put("student_id", studentId); // Add the student ID
        cv.put("timestamp", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));

        long result = db.insert("documents", null, cv);

        if (result != -1) {
            Toast.makeText(this, "Document added successfully", Toast.LENGTH_SHORT).show();
            etDocumentName.setText("");
            etDocumentDesc.setText("");
            loadDocuments(); // Refresh list
        } else {
            Toast.makeText(this, "Failed to add document", Toast.LENGTH_SHORT).show();
        }
    }
}