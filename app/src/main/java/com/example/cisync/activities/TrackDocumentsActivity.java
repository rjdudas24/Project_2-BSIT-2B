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

public class TrackDocumentsActivity extends Activity {

    private static final String TAG = "TrackDocumentsActivity";

    TextView tvPosition;
    ListView lvDocuments;
    EditText etDocumentName, etDocumentDesc;
    Button btnAddDocument;
    ImageButton btnBack;
    Spinner spFaculty;  // Faculty selection spinner
    DBHelper dbHelper;
    ArrayList<String> documentsList = new ArrayList<>();
    ArrayAdapter<String> adapter;
    String userPosition;
    int studentId = -1;
    boolean isLeadershipPosition = false;

    // Faculty selection
    ArrayList<String> facultyNames = new ArrayList<>();
    ArrayList<Integer> facultyIds = new ArrayList<>();
    int selectedFacultyId = -1; // Default to no specific faculty

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

        try {
            // Initialize DBHelper first to avoid null reference
            dbHelper = new DBHelper(this);

            // Get data from intent
            userPosition = getIntent().getStringExtra("position");
            studentId = getIntent().getIntExtra("studentId", -1);

            if (userPosition == null || userPosition.isEmpty()) {
                userPosition = "Member"; // Default
            }

            // Check if user has a leadership position
            checkLeadershipAccess();

            // Initialize views
            tvPosition = findViewById(R.id.tvUserPosition);
            lvDocuments = findViewById(R.id.lvDocuments);
            etDocumentName = findViewById(R.id.etDocumentName);
            etDocumentDesc = findViewById(R.id.etDocumentDesc);
            btnAddDocument = findViewById(R.id.btnAddDocument);
            btnBack = findViewById(R.id.btnBack);

            // Add this code to initialize faculty spinner
            spFaculty = findViewById(R.id.spFaculty);
            if (spFaculty == null) {
                // Spinner not found in layout - handle gracefully
                Log.w(TAG, "Faculty spinner not found in layout - will proceed without faculty selection");
            } else {
                loadFacultyList();
            }

            // Set back button click listener
            btnBack.setOnClickListener(v -> finish());

            // Set position text
            tvPosition.setText("Position: " + userPosition);

            // Show/hide document creation UI based on position
            View layoutAddDocument = findViewById(R.id.layoutAddDocument);
            layoutAddDocument.setVisibility(isLeadershipPosition ? View.VISIBLE : View.GONE);

            btnAddDocument.setOnClickListener(v -> addNewDocument());

            loadDocuments();

        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage(), e);
            Toast.makeText(this, "Error initializing Track Documents: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish(); // Close activity if initialization fails
        }
    }

    // Load faculty list for spinner
    private void loadFacultyList() {
        if (spFaculty == null) return; // Safety check

        facultyNames.clear();
        facultyIds.clear();

        // Add "Select Faculty" option
        facultyNames.add("All Faculty");
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

            // Set up spinner
            ArrayAdapter<String> facultyAdapter = new ArrayAdapter<>(
                    this, android.R.layout.simple_spinner_item, facultyNames);
            facultyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spFaculty.setAdapter(facultyAdapter);

            // Handle faculty selection
            spFaculty.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    selectedFacultyId = facultyIds.get(position);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    selectedFacultyId = -1; // Default to all faculty
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error loading faculty list: " + e.getMessage(), e);

            // Use default option
            ArrayAdapter<String> facultyAdapter = new ArrayAdapter<>(
                    this, android.R.layout.simple_spinner_item, facultyNames);
            facultyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spFaculty.setAdapter(facultyAdapter);
        }
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

        try {
            // Query documents safely
            String query = "SELECT * FROM documents";
            if (studentId != -1) {
                query += " WHERE student_id=" + studentId;
            }
            query += " ORDER BY id DESC";

            Cursor cursor = db.rawQuery(query, null);

            if (cursor.moveToFirst()) {
                do {
                    // Safely get column indices
                    int nameIdx = cursor.getColumnIndex("name");
                    int descIdx = cursor.getColumnIndex("description");
                    int statusIdx = cursor.getColumnIndex("status");
                    int creatorIdx = cursor.getColumnIndex("created_by");
                    int timeIdx = cursor.getColumnIndex("timestamp");

                    // Safely get values with fallbacks
                    String name = nameIdx >= 0 ? cursor.getString(nameIdx) : "Unnamed Document";
                    String desc = descIdx >= 0 ? cursor.getString(descIdx) : "No description";
                    String status = statusIdx >= 0 ? cursor.getString(statusIdx) : "Pending";
                    String creator = creatorIdx >= 0 ? cursor.getString(creatorIdx) : "Unknown";
                    String time = timeIdx >= 0 ? cursor.getString(timeIdx) : "Unknown time";

                    // Format for display - highlight status
                    String statusFormatted;
                    if ("Approved".equals(status)) {
                        statusFormatted = "✅ " + status.toUpperCase();
                    } else if ("Rejected".equals(status)) {
                        statusFormatted = "❌ " + status.toUpperCase();
                    } else {
                        statusFormatted = "⏳ " + status.toUpperCase();
                    }

                    String displayText = "Document: " + name + "\n" +
                            "Description: " + desc + "\n" +
                            "Status: " + statusFormatted + "\n" +
                            "Created by: " + creator +
                            (time != null ? " on " + time : "");

                    documentsList.add(displayText);

                } while (cursor.moveToNext());
            } else {
                // No documents found
                documentsList.add("No documents available");
            }

            cursor.close();

            adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, documentsList);
            lvDocuments.setAdapter(adapter);

        } catch (Exception e) {
            Log.e(TAG, "Error loading documents: " + e.getMessage(), e);
            documentsList.add("Error loading documents: " + e.getMessage());
            adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, documentsList);
            lvDocuments.setAdapter(adapter);
        }
    }

    private void addNewDocument() {
        try {
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

            // Start a transaction to ensure both operations succeed
            db.beginTransaction();

            try {
                // Insert the document
                ContentValues cv = new ContentValues();
                cv.put("name", name);
                cv.put("description", desc);
                cv.put("status", "Pending");
                cv.put("created_by", userPosition);
                cv.put("student_id", studentId);
                cv.put("timestamp", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));

                long documentResult = db.insert("documents", null, cv);

                if (documentResult != -1) {
                    // Create transaction record for the STUDENT who submitted the document
                    ContentValues studentTransValues = new ContentValues();
                    studentTransValues.put("user_id", studentId);
                    studentTransValues.put("action_type", "Document Submission");
                    studentTransValues.put("description", "Submitted document: " + name);
                    studentTransValues.put("timestamp", System.currentTimeMillis());

                    long studentTransResult = db.insert("transactions", null, studentTransValues);

                    if (studentTransResult != -1) {
                        Log.d(TAG, "Student transaction recorded for document submission: " + name);
                    } else {
                        Log.w(TAG, "Failed to record student transaction for document submission");
                    }

                    // Create notification for faculty (existing functionality)
                    createFacultyNotification(db, name);

                    // Commit the transaction
                    db.setTransactionSuccessful();

                    Toast.makeText(this, "Document added successfully", Toast.LENGTH_SHORT).show();
                    etDocumentName.setText("");
                    etDocumentDesc.setText("");
                    loadDocuments(); // Refresh list
                } else {
                    Toast.makeText(this, "Failed to add document", Toast.LENGTH_SHORT).show();
                }
            } finally {
                db.endTransaction(); // This will rollback if setTransactionSuccessful() wasn't called
            }

        } catch (Exception e) {
            Log.e(TAG, "Error adding document: " + e.getMessage(), e);
            Toast.makeText(this, "Error adding document: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void createFacultyNotification(SQLiteDatabase db, String documentName) {
        try {
            if (selectedFacultyId != -1 && spFaculty != null) {
                // Notification for specific faculty
                ContentValues notifyValues = new ContentValues();
                notifyValues.put("user_id", selectedFacultyId);
                notifyValues.put("action_type", "Document Submission");
                notifyValues.put("description", "New document submitted: " + documentName);
                notifyValues.put("timestamp", System.currentTimeMillis());
                notifyValues.put("read_status", 0); // Unread

                db.insert("transactions", null, notifyValues);
            } else {
                // Notify all faculty
                Cursor cursor = db.rawQuery(
                        "SELECT id FROM users WHERE role='Faculty' AND verified=1",
                        null
                );

                if (cursor.moveToFirst()) {
                    do {
                        int facultyId = cursor.getInt(0);

                        ContentValues notifyValues = new ContentValues();
                        notifyValues.put("user_id", facultyId);
                        notifyValues.put("action_type", "Document Submission");
                        notifyValues.put("description", "New document submitted: " + documentName);
                        notifyValues.put("timestamp", System.currentTimeMillis());
                        notifyValues.put("read_status", 0); // Unread

                        db.insert("transactions", null, notifyValues);
                    } while (cursor.moveToNext());
                }

                cursor.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error creating faculty notification: " + e.getMessage(), e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload documents when returning to this screen
        loadDocuments();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}