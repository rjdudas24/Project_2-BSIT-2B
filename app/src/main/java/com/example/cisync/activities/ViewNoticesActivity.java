package com.example.cisync.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import com.example.cisync.R;
import com.example.cisync.database.DBHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class ViewNoticesActivity extends Activity {

    private static final String TAG = "ViewNoticesActivity";
    ListView lvNotices;
    TextView tvNoNotices;
    ImageView btnBack;
    DBHelper dbHelper;
    ArrayList<String> noticesList = new ArrayList<>();
    ArrayList<NoticeData> noticesData = new ArrayList<>();
    ArrayAdapter<String> adapter;
    int studentId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_notices);

        try {
            // Initialize views
            lvNotices = findViewById(R.id.lvNotices);
            tvNoNotices = findViewById(R.id.tvNoNotices);
            btnBack = findViewById(R.id.btnBack);
            dbHelper = new DBHelper(this);

            // Enhance notices table if needed (safe operation)
            dbHelper.enhanceNoticesTableIfNeeded();

            // Get studentId from intent
            studentId = getIntent().getIntExtra("studentId", -1);
            Log.d(TAG, "Received studentId: " + studentId);

            if (studentId == -1) {
                Toast.makeText(this, "Error: No valid user ID provided", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            // Set up back button
            btnBack.setOnClickListener(v -> finish());

            // Load notices
            loadNotices();

            // Set up click listener for notice details
            lvNotices.setOnItemClickListener((parent, view, position, id) -> {
                if (position < noticesData.size()) {
                    NoticeData notice = noticesData.get(position);
                    showNoticeDetails(notice);
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage(), e);
            Toast.makeText(this, "Error initializing View Notices: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void loadNotices() {
        noticesList.clear();
        noticesData.clear();

        SQLiteDatabase db = null;
        Cursor cursor = null;

        try {
            db = dbHelper.getReadableDatabase();

            // First check if enhanced columns exist
            boolean hasEnhancedColumns = checkEnhancedColumns(db);

            String query;
            String[] selectionArgs;

            if (hasEnhancedColumns) {
                // Use enhanced query with targeting
                query = "SELECT n.id, n.title, n.content, n.posted_by_name, n.posted_by_position, " +
                        "n.timestamp, n.target_type, n.target_student_id " +
                        "FROM notices n " +
                        "WHERE (n.target_type = 'ALL' OR n.target_type IS NULL) OR " +
                        "(n.target_type = 'SPECIFIC' AND n.target_student_id = ?) " +
                        "ORDER BY n.id DESC";
                selectionArgs = new String[]{String.valueOf(studentId)};
            } else {
                // Use basic query with content parsing for backward compatibility
                query = "SELECT n.id, n.content, n.timestamp, n.student_id " +
                        "FROM notices n " +
                        "ORDER BY n.id DESC";
                selectionArgs = null;
            }

            cursor = db.rawQuery(query, selectionArgs);

            if (cursor.moveToFirst()) {
                tvNoNotices.setVisibility(ListView.GONE);
                lvNotices.setVisibility(ListView.VISIBLE);

                do {
                    if (hasEnhancedColumns) {
                        // Process enhanced notice data
                        int id = cursor.getInt(0);
                        String title = cursor.getString(1);
                        String content = cursor.getString(2);
                        String postedByName = cursor.getString(3);
                        String postedByPosition = cursor.getString(4);
                        String timestamp = cursor.getString(5);
                        String targetType = cursor.getString(6);
                        Integer targetStudentId = cursor.isNull(7) ? null : cursor.getInt(7);

                        NoticeData notice = new NoticeData(id, title, content, postedByName,
                                postedByPosition, timestamp, targetType, targetStudentId);
                        noticesData.add(notice);

                    } else {
                        // Process basic notice data with content parsing
                        int id = cursor.getInt(0);
                        String content = cursor.getString(1);
                        String timestamp = cursor.getString(2);
                        int posterId = cursor.getInt(3);

                        // Parse content for targeting info and title
                        ParsedNotice parsed = parseNoticeContent(content, posterId);

                        // Only include if it's for this student
                        if (parsed.isForStudent(studentId)) {
                            NoticeData notice = new NoticeData(id, parsed.title, parsed.content,
                                    parsed.postedByName, parsed.postedByPosition, timestamp,
                                    parsed.targetType, parsed.targetStudentId);
                            noticesData.add(notice);
                        }
                    }

                    // Format for display
                    if (!noticesData.isEmpty()) {
                        NoticeData lastNotice = noticesData.get(noticesData.size() - 1);
                        String displayText = formatNoticeForDisplay(lastNotice);
                        noticesList.add(displayText);
                    }

                } while (cursor.moveToNext());

            }

            if (noticesList.isEmpty()) {
                // No notices found
                tvNoNotices.setVisibility(ListView.VISIBLE);
                lvNotices.setVisibility(ListView.GONE);
                tvNoNotices.setText("No notices available");
            }

            // Update adapter
            adapter = new ArrayAdapter<>(this, R.layout.custom_list_item, noticesList);
            lvNotices.setAdapter(adapter);

        } catch (Exception e) {
            Log.e(TAG, "Error loading notices: " + e.getMessage(), e);
            Toast.makeText(this, "Error loading notices: " + e.getMessage(), Toast.LENGTH_SHORT).show();

            // Show error state
            tvNoNotices.setVisibility(ListView.VISIBLE);
            tvNoNotices.setText("Error loading notices");
            lvNotices.setVisibility(ListView.GONE);
        } finally {
            if (cursor != null) cursor.close();
            if (db != null) db.close();
        }
    }

    private boolean checkEnhancedColumns(SQLiteDatabase db) {
        try {
            Cursor cursor = db.rawQuery("PRAGMA table_info(notices)", null);
            boolean hasTargetType = false;

            if (cursor.moveToFirst()) {
                do {
                    int nameIndex = cursor.getColumnIndex("name");
                    if (nameIndex >= 0) { // FIXED: Check if column exists before using index
                        String columnName = cursor.getString(nameIndex);
                        if ("target_type".equals(columnName)) {
                            hasTargetType = true;
                            break;
                        }
                    }
                } while (cursor.moveToNext());
            }
            cursor.close();
            return hasTargetType;
        } catch (Exception e) {
            Log.e(TAG, "Error checking enhanced columns: " + e.getMessage(), e);
            return false;
        }
    }

    private ParsedNotice parseNoticeContent(String content, int posterId) {
        // Parse content format: [Position - TO: Target] Title\n\nContent
        String title = "Notice";
        String postedByPosition = "Member";
        String postedByName = "Unknown";
        String targetType = "ALL";
        Integer targetStudentId = null;
        String actualContent = content;

        try {
            // Get poster name from database
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT name, org_role FROM users WHERE id = ?",
                    new String[]{String.valueOf(posterId)});
            if (cursor.moveToFirst()) {
                postedByName = cursor.getString(0);
                String orgRole = cursor.getString(1);
                if (orgRole != null && !orgRole.isEmpty()) {
                    postedByPosition = orgRole;
                }
            }
            cursor.close();
            db.close();

            // Parse content for targeting and title
            if (content.startsWith("[") && content.contains("]")) {
                int endBracket = content.indexOf("]");
                String header = content.substring(1, endBracket);
                String remaining = content.substring(endBracket + 1).trim();

                // Extract position and target
                if (header.contains(" - TO: ")) {
                    String[] parts = header.split(" - TO: ");
                    if (parts.length == 2) {
                        postedByPosition = parts[0];
                        String target = parts[1];

                        if (!"ALL STUDENTS".equals(target)) {
                            targetType = "SPECIFIC";
                            // Try to find target student ID by name
                            targetStudentId = findStudentIdByName(target);
                        }
                    }
                }

                // Extract title and content
                if (remaining.contains("\n\n")) {
                    int doubleNewline = remaining.indexOf("\n\n");
                    title = remaining.substring(0, doubleNewline);
                    actualContent = remaining.substring(doubleNewline + 2);
                } else {
                    title = remaining;
                    actualContent = "";
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error parsing notice content: " + e.getMessage());
        }

        return new ParsedNotice(title, actualContent, postedByName, postedByPosition,
                targetType, targetStudentId);
    }

    private Integer findStudentIdByName(String name) {
        try {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT id FROM users WHERE name = ? AND role = 'Student'",
                    new String[]{name});
            Integer id = null;
            if (cursor.moveToFirst()) {
                id = cursor.getInt(0);
            }
            cursor.close();
            db.close();
            return id;
        } catch (Exception e) {
            Log.e(TAG, "Error finding student by name: " + e.getMessage(), e);
            return null;
        }
    }

    private String formatNoticeForDisplay(NoticeData notice) {
        String timeFormatted = formatTimestamp(notice.getTimestamp());
        String targetInfo = "";

        if ("SPECIFIC".equals(notice.getTargetType()) &&
                notice.getTargetStudentId() != null &&
                notice.getTargetStudentId().equals(studentId)) {
            targetInfo = " 📮 (Personal)";
        }

        return "📢 " + notice.getTitle() + targetInfo + "\n" +
                "From: " + notice.getPostedByName() + " (" + notice.getPostedByPosition() + ")\n" +
                timeFormatted;
    }

    private String formatTimestamp(String timestamp) {
        try {
            if (timestamp == null || timestamp.isEmpty()) {
                return "Unknown date";
            }
            // Try to parse and reformat timestamp
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
            Date date = inputFormat.parse(timestamp);
            return outputFormat.format(date);
        } catch (Exception e) {
            return timestamp; // Return original if parsing fails
        }
    }

    private void showNoticeDetails(NoticeData notice) {
        try {
            // Create custom dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            // Inflate custom layout
            LayoutInflater inflater = getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.dialog_notice_details, null);
            builder.setView(dialogView);

            // Get views from dialog
            TextView tvDialogNoticeTitle = dialogView.findViewById(R.id.tvDialogNoticeTitle);
            TextView tvPersonalIndicator = dialogView.findViewById(R.id.tvPersonalIndicator);
            TextView tvDialogNoticeContent = dialogView.findViewById(R.id.tvDialogNoticeContent);
            TextView tvDialogPostedBy = dialogView.findViewById(R.id.tvDialogPostedBy);
            TextView tvDialogPosition = dialogView.findViewById(R.id.tvDialogPosition);
            TextView tvDialogDatePosted = dialogView.findViewById(R.id.tvDialogDatePosted);
            TextView tvDialogTargetIcon = dialogView.findViewById(R.id.tvDialogTargetIcon);
            TextView tvDialogTarget = dialogView.findViewById(R.id.tvDialogTarget);
            TextView tvDialogNoticeId = dialogView.findViewById(R.id.tvDialogNoticeId);
            LinearLayout layoutNoticeStats = dialogView.findViewById(R.id.layoutNoticeStats);
            Button btnDialogMarkRead = dialogView.findViewById(R.id.btnDialogMarkRead);
            Button btnDialogClose = dialogView.findViewById(R.id.btnDialogClose);

            // Set notice title
            tvDialogNoticeTitle.setText(notice.getTitle().toUpperCase());

            // Check if it's a personal notice
            boolean isPersonalNotice = "SPECIFIC".equals(notice.getTargetType()) &&
                    notice.getTargetStudentId() != null &&
                    notice.getTargetStudentId().equals(studentId);

            if (isPersonalNotice) {
                tvPersonalIndicator.setVisibility(View.VISIBLE);
            } else {
                tvPersonalIndicator.setVisibility(View.GONE);
            }

            // Set notice content
            String content = notice.getContent();
            if (content == null || content.trim().isEmpty()) {
                content = "No additional content provided.";
            }
            tvDialogNoticeContent.setText(content);

            // Set poster details
            tvDialogPostedBy.setText(notice.getPostedByName());
            tvDialogPosition.setText(notice.getPostedByPosition());

            // Set date posted
            tvDialogDatePosted.setText(formatTimestamp(notice.getTimestamp()));

            // Set target audience
            if (isPersonalNotice) {
                tvDialogTargetIcon.setText("📮");
                tvDialogTarget.setText("Personal Notice");
            } else {
                tvDialogTargetIcon.setText("📢");
                tvDialogTarget.setText("All Students");
            }

            // Set notice ID (show statistics section)
            tvDialogNoticeId.setText("Notice ID: #" + notice.getId());
            layoutNoticeStats.setVisibility(View.VISIBLE);

            // Create and show dialog
            AlertDialog dialog = builder.create();

            // Set dialog properties
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }

            // Set button click listeners
            btnDialogClose.setOnClickListener(v -> dialog.dismiss());

            // Optional: Mark as read functionality (if you want to implement read tracking)
            btnDialogMarkRead.setOnClickListener(v -> {
                // Implement mark as read logic here if needed
                Toast.makeText(this, "Notice marked as read", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });

            // Show the dialog
            dialog.show();

        } catch (Exception e) {
            Log.e(TAG, "Error showing notice details: " + e.getMessage(), e);
            Toast.makeText(this, "Error showing notice details", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh notices when returning to this screen
        loadNotices();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }

    // Helper class for parsing basic notice content
    private static class ParsedNotice {
        final String title;
        final String content;
        final String postedByName;
        final String postedByPosition;
        final String targetType;
        final Integer targetStudentId;

        public ParsedNotice(String title, String content, String postedByName,
                            String postedByPosition, String targetType, Integer targetStudentId) {
            this.title = title != null ? title : "Untitled Notice";
            this.content = content != null ? content : "";
            this.postedByName = postedByName != null ? postedByName : "Unknown";
            this.postedByPosition = postedByPosition != null ? postedByPosition : "Member";
            this.targetType = targetType != null ? targetType : "ALL";
            this.targetStudentId = targetStudentId;
        }

        public boolean isForStudent(int studentId) {
            return "ALL".equals(targetType) ||
                    ("SPECIFIC".equals(targetType) && targetStudentId != null && targetStudentId == studentId);
        }
    }

    // Notice data class
    private static class NoticeData {
        private final int id;
        private final String title;
        private final String content;
        private final String postedByName;
        private final String postedByPosition;
        private final String timestamp;
        private final String targetType;
        private final Integer targetStudentId;

        public NoticeData(int id, String title, String content, String postedByName,
                          String postedByPosition, String timestamp,
                          String targetType, Integer targetStudentId) {
            this.id = id;
            this.title = title != null ? title : "Untitled Notice";
            this.content = content != null ? content : "";
            this.postedByName = postedByName != null ? postedByName : "Unknown";
            this.postedByPosition = postedByPosition != null ? postedByPosition : "Unknown";
            this.timestamp = timestamp != null ? timestamp : "";
            this.targetType = targetType != null ? targetType : "ALL";
            this.targetStudentId = targetStudentId;
        }

        public int getId() { return id; }
        public String getTitle() { return title; }
        public String getContent() { return content; }
        public String getPostedByName() { return postedByName; }
        public String getPostedByPosition() { return postedByPosition; }
        public String getTimestamp() { return timestamp; }
        public String getTargetType() { return targetType; }
        public Integer getTargetStudentId() { return targetStudentId; }
    }
}