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

public class StudentNotificationsActivity extends Activity {

    private static final String TAG = "StudentNotifications";
    ListView lvStudentNotifications;
    TextView tvNoNotifications, tvUnreadCount;
    ImageView btnBackNotifications;
    DBHelper dbHelper;
    ArrayList<String> notifications = new ArrayList<>();
    ArrayList<Integer> transactionIds = new ArrayList<>();
    ArrayList<String> actionTypes = new ArrayList<>();
    ArrayList<Boolean> readStatuses = new ArrayList<>();
    ArrayAdapter<String> adapter;
    int studentId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_notifications);

        try {
            studentId = getIntent().getIntExtra("studentId", -1);

            if (studentId == -1) {
                Log.e(TAG, "No valid student ID received");
                Toast.makeText(this, "Error: No valid student ID", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            initializeViews();
            loadNotifications();
            setupClickListeners();

        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage(), e);
            Toast.makeText(this, "Error loading notifications", Toast.LENGTH_SHORT).show();
        }
    }

    private void initializeViews() {
        lvStudentNotifications = findViewById(R.id.lvStudentNotifications);
        tvNoNotifications = findViewById(R.id.tvNoNotifications);
        tvUnreadCount = findViewById(R.id.tvUnreadCount);
        btnBackNotifications = findViewById(R.id.btnBackNotifications);
        dbHelper = new DBHelper(this);
    }

    private void setupClickListeners() {
        btnBackNotifications.setOnClickListener(v -> finish());

        lvStudentNotifications.setOnItemClickListener((parent, view, position, id) -> {
            if (position < transactionIds.size()) {
                int transactionId = transactionIds.get(position);
                String actionType = actionTypes.get(position);

                // Mark as read
                markAsRead(transactionId, position);

                // Show notification details
                showNotificationDetails(transactionId, actionType);
            }
        });
    }

    private void loadNotifications() {
        notifications.clear();
        transactionIds.clear();
        actionTypes.clear();
        readStatuses.clear();

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        int unreadCount = 0;

        try {
            String query = "SELECT t.id, t.action_type, t.description, t.timestamp, " +
                    "COALESCE(t.read_status, 0) as read_status " +
                    "FROM transactions t " +
                    "WHERE t.user_id = ? " +
                    "ORDER BY t.timestamp DESC";

            Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(studentId)});

            if (cursor.moveToFirst()) {
                do {
                    int id = cursor.getInt(0);
                    String actionType = cursor.getString(1);
                    String description = cursor.getString(2);
                    long timestamp = cursor.getLong(3);
                    int readStatus = cursor.getInt(4);

                    if (readStatus == 0) {
                        unreadCount++;
                    }

                    String formattedTime = formatTimestamp(timestamp);
                    String displayText = formatNotificationText(actionType, description, formattedTime, readStatus == 0);

                    notifications.add(displayText);
                    transactionIds.add(id);
                    actionTypes.add(actionType);
                    readStatuses.add(readStatus == 0);

                } while (cursor.moveToNext());

                // Hide empty message, show list
                tvNoNotifications.setVisibility(View.GONE);
                lvStudentNotifications.setVisibility(View.VISIBLE);

            } else {
                // No notifications found
                tvNoNotifications.setVisibility(View.VISIBLE);
                lvStudentNotifications.setVisibility(View.GONE);
                tvNoNotifications.setText("No notifications found");
            }

            cursor.close();

            // Update unread count
            if (tvUnreadCount != null) {
                tvUnreadCount.setText(unreadCount + " UNREAD");
            }

            // Use custom adapter for visual distinction
            adapter = new NotificationAdapter();
            lvStudentNotifications.setAdapter(adapter);

        } catch (Exception e) {
            Log.e(TAG, "Error loading notifications: " + e.getMessage(), e);
            Toast.makeText(this, "Error loading notifications: " + e.getMessage(), Toast.LENGTH_SHORT).show();

            // Show error state
            tvNoNotifications.setVisibility(View.VISIBLE);
            tvNoNotifications.setText("Error loading notifications");
            lvStudentNotifications.setVisibility(View.GONE);
        }
    }

    private String formatNotificationText(String actionType, String description, String time, boolean isUnread) {
        String icon = getNotificationIcon(actionType);
        String status = isUnread ? " 🔴 NEW" : "";

        return icon + " " + description + "\n" + time + status;
    }

    private String getNotificationIcon(String actionType) {
        if (actionType == null) return "📝";

        switch (actionType) {
            case "Faculty Response":
                return "💬";
            case "Document Status Update":
                return "📋";
            case "Faculty Inquiry":
                return "❓";
            case "Notice Posted":
                return "📢";
            case "Accountability Posted":
                return "💰";
            case "Accountability Status Update":
                return "💳";
            case "User Update":
                return "✏️";
            case "Registration":
                return "👤";
            default:
                return "📝";
        }
    }

    private void showNotificationDetails(int transactionId, String actionType) {
        try {
            SQLiteDatabase db = dbHelper.getReadableDatabase();

            // Get transaction details
            Cursor cursor = db.rawQuery(
                    "SELECT description, timestamp FROM transactions WHERE id=?",
                    new String[]{String.valueOf(transactionId)}
            );

            if (!cursor.moveToFirst()) {
                cursor.close();
                Toast.makeText(this, "Notification details not found", Toast.LENGTH_SHORT).show();
                return;
            }

            String description = cursor.getString(0);
            long timestamp = cursor.getLong(1);
            cursor.close();

            // Show details based on notification type
            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            if ("Faculty Response".equals(actionType)) {
                showFacultyResponseDetails(builder, description, timestamp);
            } else if ("Document Status Update".equals(actionType)) {
                showDocumentStatusDetails(builder, description, timestamp);
            } else if ("Accountability Posted".equals(actionType)) {
                showAccountabilityDetails(builder, description, timestamp);
            } else if ("Accountability Status Update".equals(actionType)) {
                showAccountabilityStatusDetails(builder, description, timestamp);
            } else {
                showGenericNotificationDetails(builder, actionType, description, timestamp);
            }

            builder.setPositiveButton("Close", null);
            builder.show();

        } catch (Exception e) {
            Log.e(TAG, "Error showing notification details: " + e.getMessage(), e);
            Toast.makeText(this, "Error showing notification details", Toast.LENGTH_SHORT).show();
        }
    }

    private void showFacultyResponseDetails(AlertDialog.Builder builder, String description, long timestamp) {
        builder.setTitle("Faculty Response");

        String responseStatus = "";
        String subject = "";

        // Parse the description to extract response and subject
        if (description.contains("'Available'")) {
            responseStatus = "✅ AVAILABLE";
        } else if (description.contains("'Unavailable'")) {
            responseStatus = "❌ UNAVAILABLE";
        }

        // Extract subject from description
        if (description.contains("to your inquiry: ")) {
            subject = description.substring(description.indexOf("to your inquiry: ") + 17);
        }

        String message = "Response Status: " + responseStatus + "\n\n" +
                "Inquiry Subject: " + subject + "\n\n" +
                "Time: " + formatTimestamp(timestamp) + "\n\n";

        if (description.contains("'Available'")) {
            message += "💡 The faculty member is available for your inquiry. You may proceed to contact them.";
        } else {
            message += "ℹ️ The faculty member is currently unavailable. Please try contacting them at a later time.";
        }

        builder.setMessage(message);
    }

    private void showDocumentStatusDetails(AlertDialog.Builder builder, String description, long timestamp) {
        builder.setTitle("Document Status Update");

        String status = "";
        String documentName = "";

        if (description.contains("has been approved")) {
            status = "✅ APPROVED";
        } else if (description.contains("has been rejected")) {
            status = "❌ REJECTED";
        }

        // Extract document name
        if (description.contains("Your document '") && description.contains("' has been")) {
            int start = description.indexOf("Your document '") + 15;
            int end = description.indexOf("' has been");
            if (start > 14 && end > start) {
                documentName = description.substring(start, end);
            }
        }

        String message = "Document: " + documentName + "\n\n" +
                "Status: " + status + "\n\n" +
                "Time: " + formatTimestamp(timestamp) + "\n\n";

        if (description.contains("approved")) {
            message += "🎉 Congratulations! Your document has been approved by the faculty.";
        } else {
            message += "📝 Your document was not approved. Please contact the faculty for more details.";
        }

        builder.setMessage(message);
    }

    private void showAccountabilityDetails(AlertDialog.Builder builder, String description, long timestamp) {
        builder.setTitle("New Accountability");

        String message = "A new accountability has been posted:\n\n" +
                description + "\n\n" +
                "Time: " + formatTimestamp(timestamp) + "\n\n" +
                "💰 Please check your Accountabilities section for payment details.";

        builder.setMessage(message);
    }

    private void showAccountabilityStatusDetails(AlertDialog.Builder builder, String description, long timestamp) {
        builder.setTitle("Accountability Status Update");

        String status = "";
        if (description.toLowerCase().contains("paid")) {
            status = "✅ PAID";
        } else if (description.toLowerCase().contains("unpaid")) {
            status = "❌ UNPAID";
        }

        String message = "Status Update: " + status + "\n\n" +
                description + "\n\n" +
                "Time: " + formatTimestamp(timestamp);

        builder.setMessage(message);
    }

    private void showGenericNotificationDetails(AlertDialog.Builder builder, String actionType, String description, long timestamp) {
        builder.setTitle("Notification Details");

        String message = "Type: " + actionType + "\n\n" +
                "Description: " + description + "\n\n" +
                "Time: " + formatTimestamp(timestamp);

        builder.setMessage(message);
    }

    private void markAsRead(int transactionId, int position) {
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();

            ContentValues values = new ContentValues();
            values.put("read_status", 1);

            int result = db.update("transactions", values, "id=?",
                    new String[]{String.valueOf(transactionId)});

            if (result > 0) {
                // Update local data
                readStatuses.set(position, false);

                // Refresh the list to update visual appearance
                adapter.notifyDataSetChanged();

                // Update unread count
                updateUnreadCount();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error marking notification as read: " + e.getMessage(), e);
        }
    }

    private void updateUnreadCount() {
        int unreadCount = 0;
        for (Boolean isUnread : readStatuses) {
            if (isUnread) unreadCount++;
        }

        if (tvUnreadCount != null) {
            tvUnreadCount.setText(unreadCount + " UNREAD");
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
            super(StudentNotificationsActivity.this, android.R.layout.simple_list_item_1, notifications);
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
    protected void onResume() {
        super.onResume();
        // Refresh notifications when returning to this activity
        loadNotifications();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}