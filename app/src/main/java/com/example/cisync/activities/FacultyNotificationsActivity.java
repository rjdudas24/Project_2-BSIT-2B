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
    ArrayList<Boolean> readStatuses = new ArrayList<>();
    ArrayList<Integer> inquiryIds = new ArrayList<>();
    DBHelper dbHelper;
    ImageView btnBackNotifications;
    TextView tvUnreadCount;
    int facultyId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faculty_notifications);

        try {
            facultyId = getIntent().getIntExtra("userId", 1);

            lvFacultyNotifications = findViewById(R.id.lvFacultyNotifications);
            btnBackNotifications = findViewById(R.id.btnBackNotifications);
            tvUnreadCount = findViewById(R.id.tvFacultyLabel);
            dbHelper = new DBHelper(this);

            loadNotifications();

            btnBackNotifications.setOnClickListener(v -> finish());

            lvFacultyNotifications.setOnItemClickListener((parent, view, position, id) -> {
                if (position < transactionIds.size() && position < actionTypes.size()) {
                    int transactionId = transactionIds.get(position);
                    String actionType = actionTypes.get(position);
                    int inquiryId = inquiryIds.get(position);

                    // Handle different types of notifications
                    if ("Faculty Inquiry".equals(actionType)) {
                        showInquiryResponseDialog(transactionId, inquiryId);
                    } else if ("Document Submission".equals(actionType)) {
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
        readStatuses.clear();
        inquiryIds.clear();

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        int unreadCount = 0;

        try {
            String query = "SELECT t.id, t.action_type, t.description, t.timestamp, " +
                    "COALESCE(t.read_status, 0) as read_status, " +
                    "COALESCE(t.inquiry_id, -1) as inquiry_id " +
                    "FROM transactions t " +
                    "WHERE t.user_id = ? " +
                    "ORDER BY t.timestamp DESC";

            Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(facultyId)});

            if (cursor.moveToFirst()) {
                do {
                    int id = cursor.getInt(0);
                    String actionType = cursor.getString(1);
                    String description = cursor.getString(2);
                    long timestamp = cursor.getLong(3);
                    int readStatus = cursor.getInt(4);
                    int inquiryId = cursor.getInt(5);

                    if (readStatus == 0) {
                        unreadCount++;
                    }

                    String formattedTime = formatTimestamp(timestamp);
                    String displayText = formatNotificationText(actionType, description, formattedTime, readStatus == 0);

                    notifications.add(displayText);
                    transactionIds.add(id);
                    actionTypes.add(actionType);
                    readStatuses.add(readStatus == 0);
                    inquiryIds.add(inquiryId);

                } while (cursor.moveToNext());
            } else {
                notifications.add("No notifications found");
                transactionIds.add(-1);
                actionTypes.add("");
                readStatuses.add(false);
                inquiryIds.add(-1);
            }

            cursor.close();

            // Update unread count
            if (tvUnreadCount != null) {
                tvUnreadCount.setText(unreadCount + " UNREAD");
            }

            // Use custom adapter to handle read/unread visual distinction
            adapter = new NotificationAdapter();
            lvFacultyNotifications.setAdapter(adapter);

        } catch (Exception e) {
            Log.e(TAG, "Error loading notifications: " + e.getMessage(), e);
            notifications.add("Error loading notifications: " + e.getMessage());
            adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, notifications);
            lvFacultyNotifications.setAdapter(adapter);
        }
    }

    private String formatNotificationText(String actionType, String description, String time, boolean isUnread) {
        String icon = getNotificationIcon(actionType);
        String status = isUnread ? " 🔴 NEW" : "";

        return icon + " " + description + "\n" + time + status;
    }

    private String getNotificationIcon(String actionType) {
        if ("Faculty Inquiry".equals(actionType)) {
            return "❓";
        } else if ("Document Submission".equals(actionType)) {
            return "📄";
        }
        return "📝";
    }

    private void showInquiryResponseDialog(int transactionId, int inquiryId) {
        if (inquiryId == -1) return;

        try {
            SQLiteDatabase db = dbHelper.getReadableDatabase();

            // Get inquiry details
            Cursor inquiryCursor = db.rawQuery(
                    "SELECT fi.student_id, fi.subject, fi.description, fi.status, u.name " +
                            "FROM faculty_inquiries fi " +
                            "JOIN users u ON fi.student_id = u.id " +
                            "WHERE fi.id = ?",
                    new String[]{String.valueOf(inquiryId)}
            );

            if (!inquiryCursor.moveToFirst()) {
                inquiryCursor.close();
                Toast.makeText(this, "Inquiry details not found", Toast.LENGTH_SHORT).show();
                return;
            }

            int studentId = inquiryCursor.getInt(0);
            String subject = inquiryCursor.getString(1);
            String description = inquiryCursor.getString(2);
            String status = inquiryCursor.getString(3);
            String studentName = inquiryCursor.getString(4);

            inquiryCursor.close();

            // Show response dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Faculty Inquiry Response");

            String message = "From: " + studentName + "\n" +
                    "Subject: " + subject + "\n\n" +
                    "Description: " + description + "\n\n" +
                    "Current Status: " + status + "\n\n" +
                    "Choose your response:";

            builder.setMessage(message);

            builder.setPositiveButton("Available", (dialog, which) -> {
                respondToInquiry(inquiryId, studentId, "Available", subject, studentName);
            });

            builder.setNegativeButton("Unavailable", (dialog, which) -> {
                respondToInquiry(inquiryId, studentId, "Unavailable", subject, studentName);
            });

            builder.setNeutralButton("Close", null);
            builder.show();

        } catch (Exception e) {
            Log.e(TAG, "Error showing inquiry response dialog: " + e.getMessage(), e);
            Toast.makeText(this, "Error showing inquiry details", Toast.LENGTH_SHORT).show();
        }
    }

    private void respondToInquiry(int inquiryId, int studentId, String response, String subject, String studentName) {
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            db.beginTransaction();

            // Update inquiry status
            ContentValues inquiryUpdate = new ContentValues();
            inquiryUpdate.put("status", response);
            inquiryUpdate.put("response_time", System.currentTimeMillis());

            int updateResult = db.update("faculty_inquiries", inquiryUpdate, "id=?",
                    new String[]{String.valueOf(inquiryId)});

            if (updateResult > 0) {
                // Create notification for student
                ContentValues studentNotification = new ContentValues();
                studentNotification.put("user_id", studentId);
                studentNotification.put("action_type", "Faculty Response");
                studentNotification.put("description", "Faculty responded '" + response + "' to your inquiry: " + subject);
                studentNotification.put("timestamp", System.currentTimeMillis());
                studentNotification.put("read_status", 0);

                db.insert("transactions", null, studentNotification);

                // Record faculty response in faculty's transaction history
                ContentValues facultyTransaction = new ContentValues();
                facultyTransaction.put("user_id", facultyId);
                facultyTransaction.put("action_type", "Faculty Inquiry Response");
                facultyTransaction.put("description", "Responded '" + response + "' to inquiry from " + studentName + ": " + subject);
                facultyTransaction.put("timestamp", System.currentTimeMillis());

                db.insert("transactions", null, facultyTransaction);

                db.setTransactionSuccessful();

                Toast.makeText(this, "Response sent to student", Toast.LENGTH_SHORT).show();
                loadNotifications(); // Refresh notifications

            } else {
                Toast.makeText(this, "Error sending response", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error responding to inquiry: " + e.getMessage(), e);
            Toast.makeText(this, "Error sending response", Toast.LENGTH_SHORT).show();
        } finally {
            try {
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                if (db.inTransaction()) {
                    db.endTransaction();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void showDocumentDetails(int transactionId) {
        try {
            SQLiteDatabase db = dbHelper.getReadableDatabase();

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

            String documentName = description.replace("New document submitted: ", "").trim();

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

            String studentName = "Unknown Student";
            Cursor studentCursor = db.rawQuery(
                    "SELECT name FROM users WHERE id=?",
                    new String[]{String.valueOf(studentId)}
            );

            if (studentCursor.moveToFirst()) {
                studentName = studentCursor.getString(0);
            }
            studentCursor.close();

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Document Details");

            String message = "Document: " + name + "\n\n" +
                    "Description: " + desc + "\n\n" +
                    "Status: " + status + "\n\n" +
                    "Submitted by: " + studentName + " (" + createdBy + ")\n" +
                    "Date: " + timestamp;

            builder.setMessage(message);

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
            db.beginTransaction();

            ContentValues values = new ContentValues();
            values.put("status", status);

            int updateResult = db.update("documents", values, "id=?", new String[]{String.valueOf(docId)});

            if (updateResult > 0) {
                ContentValues transValues = new ContentValues();
                transValues.put("user_id", studentId);
                transValues.put("action_type", "Document Status Update");
                transValues.put("description", "Your document '" + docName + "' has been " + status.toLowerCase());
                transValues.put("timestamp", System.currentTimeMillis());
                transValues.put("read_status", 0);

                long transResult = db.insert("transactions", null, transValues);

                if (transResult != -1) {
                    db.setTransactionSuccessful();
                    Toast.makeText(this, "Document " + status.toLowerCase(), Toast.LENGTH_SHORT).show();
                    loadNotifications();
                } else {
                    Toast.makeText(this, "Error updating document status", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Error updating document status", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error updating document status: " + e.getMessage(), e);
            Toast.makeText(this, "Error updating document status", Toast.LENGTH_SHORT).show();
        } finally {
            try {
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                if (db.inTransaction()) {
                    db.endTransaction();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void markAsRead(int transactionId) {
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();

            ContentValues values = new ContentValues();
            values.put("read_status", 1);

            db.update("transactions", values, "id=?", new String[]{String.valueOf(transactionId)});
            loadNotifications(); // Refresh to update visual state

        } catch (Exception e) {
            Log.e(TAG, "Error marking notification as read: " + e.getMessage(), e);
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

    // Custom adapter to handle visual distinction between read and unread notifications
    private class NotificationAdapter extends ArrayAdapter<String> {
        public NotificationAdapter() {
            super(FacultyNotificationsActivity.this, android.R.layout.simple_list_item_1, notifications);
        }

        @Override
        public View getView(int position, View convertView, android.view.ViewGroup parent) {
            View view = super.getView(position, convertView, parent);
            TextView textView = view.findViewById(android.R.id.text1);

            // Visual distinction for unread notifications
            if (position < readStatuses.size() && readStatuses.get(position)) {
                textView.setTextColor(getResources().getColor(android.R.color.white));
                textView.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_dark));
                textView.setTypeface(null, android.graphics.Typeface.BOLD);
            } else {
                textView.setTextColor(getResources().getColor(android.R.color.white));
                textView.setBackgroundColor(getResources().getColor(android.R.color.transparent));
                textView.setTypeface(null, android.graphics.Typeface.NORMAL);
            }

            textView.setPadding(16, 12, 16, 12);
            return view;
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