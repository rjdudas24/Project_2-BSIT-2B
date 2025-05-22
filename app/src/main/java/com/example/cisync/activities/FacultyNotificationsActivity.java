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
import java.util.ArrayList;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FacultyNotificationsActivity extends Activity {

    private static final String TAG = "FacultyNotifications";
    ListView lvFacultyNotifications;
    ArrayAdapter<String> adapter;
    ArrayList<String> notifications = new ArrayList<>();
    ArrayList<Integer> transactionIds = new ArrayList<>();
    ArrayList<String> actionTypes = new ArrayList<>();
    DBHelper dbHelper;
    ImageView btnBackNotifications;
    int facultyId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faculty_notifications);

        try {
            // Get faculty ID from intent
            facultyId = getIntent().getIntExtra("userId", 1);

            lvFacultyNotifications = findViewById(R.id.lvFacultyNotifications);
            btnBackNotifications = findViewById(R.id.btnBackNotifications);
            dbHelper = new DBHelper(this);

            // Load notifications
            loadNotifications();

            // Set back button click listener
            btnBackNotifications.setOnClickListener(v -> finish());

            // Set item click listener for notifications
            lvFacultyNotifications.setOnItemClickListener((parent, view, position, id) -> {
                if (position < transactionIds.size() && position < actionTypes.size()) {
                    int transactionId = transactionIds.get(position);
                    String actionType = actionTypes.get(position);

                    // Handle different types of notifications
                    if ("Document Submission".equals(actionType)) {
                        showDocumentDetails(transactionId);
                    }

                    // Mark as read
                    markAsRead(transactionId);
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage(), e);
            Toast.makeText(this, "Error loading notifications", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadNotifications() {
        notifications.clear();
        transactionIds.clear();
        actionTypes.clear();

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        int unreadCount = 0;

        try {
            // Get column index to check if read_status exists
            boolean hasReadStatus = false;
            Cursor columnsCursor = db.rawQuery("PRAGMA table_info(transactions)", null);
            if (columnsCursor.moveToFirst()) {
                do {
                    int nameIndex = columnsCursor.getColumnIndex("name");
                    if (nameIndex >= 0 && columnsCursor.getString(nameIndex).equals("read_status")) {
                        hasReadStatus = true;
                        break;
                    }
                } while (columnsCursor.moveToNext());
            }
            columnsCursor.close();

            // Query notifications
            String query = "SELECT id, action_type, description, timestamp";
            if (hasReadStatus) {
                query += ", read_status";
            }
            query += " FROM transactions WHERE user_id = ? ORDER BY timestamp DESC";

            Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(facultyId)});

            if (cursor.moveToFirst()) {
                do {
                    int id = cursor.getInt(0);
                    String actionType = cursor.getString(1);
                    String description = cursor.getString(2);
                    long timestamp = cursor.getLong(3);
                    int readStatus = 0; // Default to unread

                    // Check if read_status exists
                    if (hasReadStatus) {
                        int readStatusIdx = cursor.getColumnIndex("read_status");
                        if (readStatusIdx >= 0) {
                            readStatus = cursor.getInt(readStatusIdx);
                        }
                    }

                    if (readStatus == 0) {
                        unreadCount++;
                    }

                    // Format timestamp
                    String formattedTime = formatTimestamp(timestamp);

                    // Format display text
                    String displayText;
                    if ("Document Submission".equals(actionType)) {
                        displayText = "📄 " + description + "\n" + formattedTime;
                        if (readStatus == 0) {
                            displayText += " (NEW)";
                        }
                    } else {
                        displayText = description + "\n(" + formattedTime + ")";
                        if (readStatus == 0) {
                            displayText += " (NEW)";
                        }
                    }

                    notifications.add(displayText);
                    transactionIds.add(id);
                    actionTypes.add(actionType);

                } while (cursor.moveToNext());
            } else {
                notifications.add("No notifications found");
            }

            cursor.close();

            // Update UI with unread count if needed
            try {
                TextView tvUnread = findViewById(R.id.tvFacultyLabel);
                if (tvUnread != null) {
                    tvUnread.setText(unreadCount + " UNREAD");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error updating unread count: " + e.getMessage());
            }

            adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, notifications);
            lvFacultyNotifications.setAdapter(adapter);
        } catch (Exception e) {
            Log.e(TAG, "Error loading notifications: " + e.getMessage(), e);
            notifications.add("Error loading notifications: " + e.getMessage());
            adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, notifications);
            lvFacultyNotifications.setAdapter(adapter);
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

    private void showDocumentDetails(int transactionId) {
        try {
            SQLiteDatabase db = dbHelper.getReadableDatabase();

            // Get transaction details
            Cursor transCursor = db.rawQuery(
                    "SELECT description FROM transactions WHERE id=?",
                    new String[]{String.valueOf(transactionId)}
            );

            if (!transCursor.moveToFirst()) {
                transCursor.close();
                Toast.makeText(this, "Notification details not found", Toast.LENGTH_SHORT).show();
                return;
            }

            String description = transCursor.getString(0);
            transCursor.close();

            // Extract document name from description (assumed format: "New document submitted: [Name]")
            String documentName = description.replace("New document submitted: ", "").trim();

            // Find document details
            Cursor docCursor = db.rawQuery(
                    "SELECT id, name, description, status, created_by, timestamp, student_id FROM documents WHERE name=?",
                    new String[]{documentName}
            );

            if (!docCursor.moveToFirst()) {
                docCursor.close();
                Toast.makeText(this, "Document not found", Toast.LENGTH_SHORT).show();
                return;
            }

            int docId = docCursor.getInt(0);
            String name = docCursor.getString(1);
            String desc = docCursor.getString(2);
            String status = docCursor.getString(3);
            String createdBy = docCursor.getString(4);
            String timestamp = docCursor.getString(5);
            int studentId = docCursor.getInt(6);

            docCursor.close();

            // Get student name
            String studentName = "Unknown Student";
            Cursor studentCursor = db.rawQuery(
                    "SELECT name FROM users WHERE id=?",
                    new String[]{String.valueOf(studentId)}
            );

            if (studentCursor.moveToFirst()) {
                studentName = studentCursor.getString(0);
            }
            studentCursor.close();

            // Show dialog with document details
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Document Details");

            String message = "Document: " + name + "\n\n" +
                    "Description: " + desc + "\n\n" +
                    "Status: " + status + "\n\n" +
                    "Submitted by: " + studentName + " (" + createdBy + ")\n" +
                    "Date: " + timestamp;

            builder.setMessage(message);

            // Add action buttons for document approval/rejection
            builder.setPositiveButton("Approve", (dialog, which) -> {
                updateDocumentStatus(docId, "Approved", studentId, name);
            });

            builder.setNegativeButton("Reject", (dialog, which) -> {
                updateDocumentStatus(docId, "Rejected", studentId, name);
            });

            builder.setNeutralButton("Close", null);

            builder.show();

        } catch (Exception e) {
            Log.e(TAG, "Error showing document details: " + e.getMessage(), e);
            Toast.makeText(this, "Error showing document details", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateDocumentStatus(int docId, String status, int studentId, String docName) {
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();

            // Start transaction to ensure both operations succeed
            db.beginTransaction();

            try {
                // Update document status
                ContentValues values = new ContentValues();
                values.put("status", status);

                int updateResult = db.update("documents", values, "id=?", new String[]{String.valueOf(docId)});

                if (updateResult > 0) {
                    // Create notification for student about status change
                    ContentValues transValues = new ContentValues();
                    transValues.put("user_id", studentId);
                    transValues.put("action_type", "Document Status Update");
                    transValues.put("description", "Your document '" + docName + "' has been " + status.toLowerCase());
                    transValues.put("timestamp", System.currentTimeMillis());

                    // Add read_status if column exists
                    try {
                        transValues.put("read_status", 0);
                    } catch (Exception e) {
                        // Column may not exist, continue
                    }

                    long transResult = db.insert("transactions", null, transValues);

                    if (transResult != -1) {
                        Log.d(TAG, "Student transaction created for document status update: " + docName + " -> " + status);

                        // Commit the transaction
                        db.setTransactionSuccessful();

                        Toast.makeText(this, "Document " + status.toLowerCase(), Toast.LENGTH_SHORT).show();

                        // Refresh notifications
                        loadNotifications();
                    } else {
                        Log.e(TAG, "Failed to create student transaction for document status update");
                        Toast.makeText(this, "Error updating document status", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e(TAG, "Failed to update document status in database");
                    Toast.makeText(this, "Error updating document status", Toast.LENGTH_SHORT).show();
                }
            } finally {
                db.endTransaction(); // This will rollback if setTransactionSuccessful() wasn't called
            }

        } catch (Exception e) {
            Log.e(TAG, "Error updating document status: " + e.getMessage(), e);
            Toast.makeText(this, "Error updating document status", Toast.LENGTH_SHORT).show();
        }
    }

    private void markAsRead(int transactionId) {
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();

            // Check if read_status column exists
            Cursor columnsCursor = db.rawQuery("PRAGMA table_info(transactions)", null);
            boolean hasReadStatus = false;

            if (columnsCursor.moveToFirst()) {
                do {
                    int nameIndex = columnsCursor.getColumnIndex("name");
                    if (nameIndex >= 0 && columnsCursor.getString(nameIndex).equals("read_status")) {
                        hasReadStatus = true;
                        break;
                    }
                } while (columnsCursor.moveToNext());
            }
            columnsCursor.close();

            if (hasReadStatus) {
                ContentValues values = new ContentValues();
                values.put("read_status", 1);

                db.update("transactions", values, "id=?",
                        new String[]{String.valueOf(transactionId)});

                // Refresh notifications
                loadNotifications();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error marking notification as read: " + e.getMessage(), e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed(); // Ensure system back button works too
    }
}