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

        return "\n" + icon + " " + description + "\n" + time + status + "\n";
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

            // Create custom dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            // Inflate custom layout
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_notification_details, null);
            builder.setView(dialogView);

            // Create dialog
            AlertDialog dialog = builder.create();

            // Make dialog background transparent to show custom background
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }

            // Find views in custom layout
            ImageView ivNotificationIcon = dialogView.findViewById(R.id.ivNotificationIcon);
            TextView tvDialogTitle = dialogView.findViewById(R.id.tvDialogTitle);
            View vUnreadIndicator = dialogView.findViewById(R.id.vUnreadIndicator);
            ImageView ivNotificationTypeIcon = dialogView.findViewById(R.id.ivNotificationTypeIcon);
            TextView tvNotificationType = dialogView.findViewById(R.id.tvNotificationType);
            TextView tvNotificationStatus = dialogView.findViewById(R.id.tvNotificationStatus);
            LinearLayout llSubjectRow = dialogView.findViewById(R.id.llSubjectRow);
            TextView tvSubjectLabel = dialogView.findViewById(R.id.tvSubjectLabel);
            TextView tvSubjectValue = dialogView.findViewById(R.id.tvSubjectValue);
            TextView tvNotificationDescription = dialogView.findViewById(R.id.tvNotificationDescription);
            TextView tvNotificationTime = dialogView.findViewById(R.id.tvNotificationTime);
            LinearLayout llAdditionalInfo = dialogView.findViewById(R.id.llAdditionalInfo);
            ImageView ivAdditionalIcon = dialogView.findViewById(R.id.ivAdditionalIcon);
            TextView tvAdditionalInfo = dialogView.findViewById(R.id.tvAdditionalInfo);
            Button btnMarkAsRead = dialogView.findViewById(R.id.btnMarkAsRead);
            Button btnDialogClose = dialogView.findViewById(R.id.btnDialogClose);

            // Set basic information
            tvNotificationType.setText(actionType);
            tvNotificationDescription.setText(description);
            tvNotificationTime.setText(formatTimestamp(timestamp));

            // Check if this notification is unread
            int position = transactionIds.indexOf(transactionId);
            boolean isUnread = position >= 0 && position < readStatuses.size() && readStatuses.get(position);

            if (isUnread) {
                vUnreadIndicator.setVisibility(View.VISIBLE);
                btnMarkAsRead.setVisibility(View.VISIBLE);
            } else {
                vUnreadIndicator.setVisibility(View.GONE);
                btnMarkAsRead.setVisibility(View.GONE);
            }

            // Configure dialog based on notification type
            switch (actionType) {
                case "Faculty Response":
                    configureFacultyResponseDialog(dialogView, description, timestamp);
                    break;
                case "Document Status Update":
                    configureDocumentStatusDialog(dialogView, description, timestamp);
                    break;
                case "Accountability Posted":
                    configureAccountabilityDialog(dialogView, description, timestamp);
                    break;
                case "Accountability Status Update":
                    configureAccountabilityStatusDialog(dialogView, description, timestamp);
                    break;
                case "Faculty Inquiry":
                    configureFacultyInquiryDialog(dialogView, description, timestamp);
                    break;
                case "Notice Posted":
                    configureNoticeDialog(dialogView, description, timestamp);
                    break;
                case "User Update":
                    configureUserUpdateDialog(dialogView, description, timestamp);
                    break;
                case "Registration":
                    configureRegistrationDialog(dialogView, description, timestamp);
                    break;
                default:
                    configureGenericDialog(dialogView, actionType, description, timestamp);
                    break;
            }

            // Set click listeners
            btnMarkAsRead.setOnClickListener(v -> {
                markAsRead(transactionId, position);
                vUnreadIndicator.setVisibility(View.GONE);
                btnMarkAsRead.setVisibility(View.GONE);
                Toast.makeText(this, "Marked as read", Toast.LENGTH_SHORT).show();
            });

            btnDialogClose.setOnClickListener(v -> dialog.dismiss());

            // Show dialog
            dialog.show();

        } catch (Exception e) {
            Log.e(TAG, "Error showing custom notification details dialog: " + e.getMessage(), e);

            // Fallback to simple dialog
            showFallbackNotificationDialog(transactionId, actionType);
        }
    }

    private void configureFacultyResponseDialog(View dialogView, String description, long timestamp) {
        ImageView ivNotificationIcon = dialogView.findViewById(R.id.ivNotificationIcon);
        ImageView ivNotificationTypeIcon = dialogView.findViewById(R.id.ivNotificationTypeIcon);
        TextView tvNotificationStatus = dialogView.findViewById(R.id.tvNotificationStatus);
        LinearLayout llSubjectRow = dialogView.findViewById(R.id.llSubjectRow);
        TextView tvSubjectLabel = dialogView.findViewById(R.id.tvSubjectLabel);
        TextView tvSubjectValue = dialogView.findViewById(R.id.tvSubjectValue);
        LinearLayout llAdditionalInfo = dialogView.findViewById(R.id.llAdditionalInfo);
        ImageView ivAdditionalIcon = dialogView.findViewById(R.id.ivAdditionalIcon);
        TextView tvAdditionalInfo = dialogView.findViewById(R.id.tvAdditionalInfo);

        // Set icons
        ivNotificationIcon.setImageResource(R.drawable.notification);
        ivNotificationTypeIcon.setImageResource(R.drawable.ic_user);

        // Parse response status
        String responseStatus = "";
        if (description.contains("'Available'")) {
            responseStatus = "✅ AVAILABLE";
            tvNotificationStatus.setBackgroundColor(getResources().getColor(android.R.color.holo_green_light));
        } else if (description.contains("'Unavailable'")) {
            responseStatus = "❌ UNAVAILABLE";
            tvNotificationStatus.setBackgroundColor(getResources().getColor(android.R.color.holo_red_light));
        }

        if (!responseStatus.isEmpty()) {
            tvNotificationStatus.setText(responseStatus);
            tvNotificationStatus.setVisibility(View.VISIBLE);
        }

        // Extract subject from description
        if (description.contains("to your inquiry: ")) {
            String subject = description.substring(description.indexOf("to your inquiry: ") + 17);
            llSubjectRow.setVisibility(View.VISIBLE);
            tvSubjectLabel.setText("Inquiry Subject:");
            tvSubjectValue.setText(subject);
        }

        // Show additional information
        llAdditionalInfo.setVisibility(View.VISIBLE);
        ivAdditionalIcon.setImageResource(R.drawable.notice_icon);

        if (description.contains("'Available'")) {
            tvAdditionalInfo.setText("💡 The faculty member is available for your inquiry. You may proceed to contact them.");
        } else {
            tvAdditionalInfo.setText("ℹ️ The faculty member is currently unavailable. Please try contacting them at a later time.");
        }
    }

    private void configureDocumentStatusDialog(View dialogView, String description, long timestamp) {
        ImageView ivNotificationIcon = dialogView.findViewById(R.id.ivNotificationIcon);
        ImageView ivNotificationTypeIcon = dialogView.findViewById(R.id.ivNotificationTypeIcon);
        TextView tvNotificationStatus = dialogView.findViewById(R.id.tvNotificationStatus);
        LinearLayout llSubjectRow = dialogView.findViewById(R.id.llSubjectRow);
        TextView tvSubjectLabel = dialogView.findViewById(R.id.tvSubjectLabel);
        TextView tvSubjectValue = dialogView.findViewById(R.id.tvSubjectValue);
        LinearLayout llAdditionalInfo = dialogView.findViewById(R.id.llAdditionalInfo);
        ImageView ivAdditionalIcon = dialogView.findViewById(R.id.ivAdditionalIcon);
        TextView tvAdditionalInfo = dialogView.findViewById(R.id.tvAdditionalInfo);

        // Set icons
        ivNotificationIcon.setImageResource(R.drawable.notification);
        ivNotificationTypeIcon.setImageResource(R.drawable.document_icon);

        // Parse status
        String status = "";
        if (description.contains("has been approved")) {
            status = "✅ APPROVED";
            tvNotificationStatus.setBackgroundColor(getResources().getColor(android.R.color.holo_green_light));
        } else if (description.contains("has been rejected")) {
            status = "❌ REJECTED";
            tvNotificationStatus.setBackgroundColor(getResources().getColor(android.R.color.holo_red_light));
        }

        if (!status.isEmpty()) {
            tvNotificationStatus.setText(status);
            tvNotificationStatus.setVisibility(View.VISIBLE);
        }

        // Extract document name
        if (description.contains("Your document '") && description.contains("' has been")) {
            int start = description.indexOf("Your document '") + 15;
            int end = description.indexOf("' has been");
            if (start > 14 && end > start) {
                String documentName = description.substring(start, end);
                llSubjectRow.setVisibility(View.VISIBLE);
                tvSubjectLabel.setText("Document:");
                tvSubjectValue.setText(documentName);
            }
        }

        // Show additional information
        llAdditionalInfo.setVisibility(View.VISIBLE);
        ivAdditionalIcon.setImageResource(R.drawable.notice_icon);

        if (description.contains("approved")) {
            tvAdditionalInfo.setText("🎉 Congratulations! Your document has been approved by the faculty.");
        } else {
            tvAdditionalInfo.setText("📝 Your document was not approved. Please contact the faculty for more details.");
        }
    }

    private void configureAccountabilityDialog(View dialogView, String description, long timestamp) {
        ImageView ivNotificationIcon = dialogView.findViewById(R.id.ivNotificationIcon);
        ImageView ivNotificationTypeIcon = dialogView.findViewById(R.id.ivNotificationTypeIcon);
        LinearLayout llAdditionalInfo = dialogView.findViewById(R.id.llAdditionalInfo);
        ImageView ivAdditionalIcon = dialogView.findViewById(R.id.ivAdditionalIcon);
        TextView tvAdditionalInfo = dialogView.findViewById(R.id.tvAdditionalInfo);

        // Set icons
        ivNotificationIcon.setImageResource(R.drawable.notification);
        ivNotificationTypeIcon.setImageResource(R.drawable.wallet);

        // Show additional information
        llAdditionalInfo.setVisibility(View.VISIBLE);
        ivAdditionalIcon.setImageResource(R.drawable.notice_icon);
        tvAdditionalInfo.setText("💰 Please check your Accountabilities section for payment details.");
    }

    private void configureAccountabilityStatusDialog(View dialogView, String description, long timestamp) {
        ImageView ivNotificationIcon = dialogView.findViewById(R.id.ivNotificationIcon);
        ImageView ivNotificationTypeIcon = dialogView.findViewById(R.id.ivNotificationTypeIcon);
        TextView tvNotificationStatus = dialogView.findViewById(R.id.tvNotificationStatus);

        // Set icons
        ivNotificationIcon.setImageResource(R.drawable.notification);
        ivNotificationTypeIcon.setImageResource(R.drawable.wallet);

        // Parse status
        String status = "";
        if (description.toLowerCase().contains("paid")) {
            status = "✅ PAID";
            tvNotificationStatus.setBackgroundColor(getResources().getColor(android.R.color.holo_green_light));
        } else if (description.toLowerCase().contains("unpaid")) {
            status = "❌ UNPAID";
            tvNotificationStatus.setBackgroundColor(getResources().getColor(android.R.color.holo_red_light));
        }

        if (!status.isEmpty()) {
            tvNotificationStatus.setText(status);
            tvNotificationStatus.setVisibility(View.VISIBLE);
        }
    }

    private void configureFacultyInquiryDialog(View dialogView, String description, long timestamp) {
        ImageView ivNotificationIcon = dialogView.findViewById(R.id.ivNotificationIcon);
        ImageView ivNotificationTypeIcon = dialogView.findViewById(R.id.ivNotificationTypeIcon);
        LinearLayout llAdditionalInfo = dialogView.findViewById(R.id.llAdditionalInfo);
        ImageView ivAdditionalIcon = dialogView.findViewById(R.id.ivAdditionalIcon);
        TextView tvAdditionalInfo = dialogView.findViewById(R.id.tvAdditionalInfo);

        // Set icons
        ivNotificationIcon.setImageResource(R.drawable.notification);
        ivNotificationTypeIcon.setImageResource(R.drawable.ic_user);

        // Show additional information
        llAdditionalInfo.setVisibility(View.VISIBLE);
        ivAdditionalIcon.setImageResource(R.drawable.notice_icon);
        tvAdditionalInfo.setText("❓ A faculty member has sent you an inquiry. Please respond at your earliest convenience.");
    }

    private void configureNoticeDialog(View dialogView, String description, long timestamp) {
        ImageView ivNotificationIcon = dialogView.findViewById(R.id.ivNotificationIcon);
        ImageView ivNotificationTypeIcon = dialogView.findViewById(R.id.ivNotificationTypeIcon);
        LinearLayout llAdditionalInfo = dialogView.findViewById(R.id.llAdditionalInfo);
        ImageView ivAdditionalIcon = dialogView.findViewById(R.id.ivAdditionalIcon);
        TextView tvAdditionalInfo = dialogView.findViewById(R.id.tvAdditionalInfo);

        // Set icons
        ivNotificationIcon.setImageResource(R.drawable.notification);
        ivNotificationTypeIcon.setImageResource(R.drawable.notice_icon);

        // Show additional information
        llAdditionalInfo.setVisibility(View.VISIBLE);
        ivAdditionalIcon.setImageResource(R.drawable.notice_icon);
        tvAdditionalInfo.setText("📢 A new notice has been posted. Please read it carefully for important information.");
    }

    private void configureUserUpdateDialog(View dialogView, String description, long timestamp) {
        ImageView ivNotificationIcon = dialogView.findViewById(R.id.ivNotificationIcon);
        ImageView ivNotificationTypeIcon = dialogView.findViewById(R.id.ivNotificationTypeIcon);
        LinearLayout llAdditionalInfo = dialogView.findViewById(R.id.llAdditionalInfo);
        ImageView ivAdditionalIcon = dialogView.findViewById(R.id.ivAdditionalIcon);
        TextView tvAdditionalInfo = dialogView.findViewById(R.id.tvAdditionalInfo);

        // Set icons
        ivNotificationIcon.setImageResource(R.drawable.notification);
        ivNotificationTypeIcon.setImageResource(R.drawable.ic_user);

        // Show additional information
        llAdditionalInfo.setVisibility(View.VISIBLE);
        ivAdditionalIcon.setImageResource(R.drawable.notice_icon);
        tvAdditionalInfo.setText("✏️ Your user information has been updated. Please review the changes.");
    }

    private void configureRegistrationDialog(View dialogView, String description, long timestamp) {
        ImageView ivNotificationIcon = dialogView.findViewById(R.id.ivNotificationIcon);
        ImageView ivNotificationTypeIcon = dialogView.findViewById(R.id.ivNotificationTypeIcon);
        LinearLayout llAdditionalInfo = dialogView.findViewById(R.id.llAdditionalInfo);
        ImageView ivAdditionalIcon = dialogView.findViewById(R.id.ivAdditionalIcon);
        TextView tvAdditionalInfo = dialogView.findViewById(R.id.tvAdditionalInfo);

        // Set icons
        ivNotificationIcon.setImageResource(R.drawable.notification);
        ivNotificationTypeIcon.setImageResource(R.drawable.ic_user);

        // Show additional information
        llAdditionalInfo.setVisibility(View.VISIBLE);
        ivAdditionalIcon.setImageResource(R.drawable.notice_icon);
        tvAdditionalInfo.setText("👤 Welcome! Your registration has been processed successfully.");
    }

    private void configureGenericDialog(View dialogView, String actionType, String description, long timestamp) {
        ImageView ivNotificationIcon = dialogView.findViewById(R.id.ivNotificationIcon);
        ImageView ivNotificationTypeIcon = dialogView.findViewById(R.id.ivNotificationTypeIcon);

        // Set default icons
        ivNotificationIcon.setImageResource(R.drawable.notification);
        ivNotificationTypeIcon.setImageResource(R.drawable.notification);
    }

    private void showFallbackNotificationDialog(int transactionId, String actionType) {
        try {
            SQLiteDatabase db = dbHelper.getReadableDatabase();

            Cursor cursor = db.rawQuery(
                    "SELECT description, timestamp FROM transactions WHERE id=?",
                    new String[]{String.valueOf(transactionId)}
            );

            if (!cursor.moveToFirst()) {
                cursor.close();
                return;
            }

            String description = cursor.getString(0);
            long timestamp = cursor.getLong(1);
            cursor.close();

            AlertDialog.Builder fallbackBuilder = new AlertDialog.Builder(this);
            fallbackBuilder.setTitle("Notification Details");

            String message = "Type: " + actionType + "\n\n" +
                    "Description: " + description + "\n\n" +
                    "Time: " + formatTimestamp(timestamp);

            fallbackBuilder.setMessage(message);
            fallbackBuilder.setPositiveButton("Close", null);
            fallbackBuilder.show();

            Toast.makeText(this, "Using fallback dialog due to error", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Log.e(TAG, "Error showing fallback notification dialog: " + e.getMessage(), e);
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
            super(StudentNotificationsActivity.this, R.layout.notif_list, notifications);
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