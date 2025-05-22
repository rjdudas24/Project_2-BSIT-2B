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
import java.util.*;

public class FacultyHistoryActivity extends Activity {
    private static final String TAG = "FacultyHistory";
    ListView lvHistory;
    ImageView btnBackHistory;
    Spinner spinnerFilter;
    Button btnApplyFilter, btnClearFilter;

    ArrayList<String> historyList = new ArrayList<>();
    ArrayList<Integer> transactionIds = new ArrayList<>();
    ArrayList<Integer> targetUserIds = new ArrayList<>(); // Changed from studentIds
    ArrayList<String> messages = new ArrayList<>();
    ArrayList<String> actionTypes = new ArrayList<>();
    ArrayList<Integer> inquiryIds = new ArrayList<>(); // For inquiry re-responses
    ArrayList<String> documentNames = new ArrayList<>(); // For document re-responses
    ArrayAdapter<String> adapter;
    DBHelper dbHelper;
    int facultyId;
    String currentFilter = "All";

    // Define which action types can be re-responded to
    private static final Set<String> RE_RESPONDABLE_ACTIONS = new HashSet<>(Arrays.asList(
            "Faculty Inquiry Response",
            "Document Status Update"
    ));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faculty_history);

        facultyId = getIntent().getIntExtra("userId", -1);
        if (facultyId == -1) {
            facultyId = 1; // Default faculty ID
            Log.d(TAG, "Using default faculty ID: " + facultyId);
        } else {
            Log.d(TAG, "Using provided faculty ID: " + facultyId);
        }

        initializeViews();
        setupFilterSpinner();
        setupClickListeners();
        loadHistory(null);
    }

    private void initializeViews() {
        lvHistory = findViewById(R.id.lvHistory);
        btnBackHistory = findViewById(R.id.btnBackHistory);
        spinnerFilter = findViewById(R.id.spinnerFilter);
        btnApplyFilter = findViewById(R.id.btnApplyFilter);
        btnClearFilter = findViewById(R.id.btnClearFilter);
        dbHelper = new DBHelper(this);
    }

    private void setupFilterSpinner() {
        try {
            ArrayList<String> filterOptions = new ArrayList<>();
            filterOptions.add("All");
            filterOptions.add("Faculty Inquiry Response");
            filterOptions.add("Document Status Update");
            filterOptions.add("Faculty Inquiry");
            filterOptions.add("Document Submission");

            ArrayAdapter<String> filterAdapter = new ArrayAdapter<>(
                    this, R.layout.custom_spinner_white, filterOptions);
            filterAdapter.setDropDownViewResource(R.layout.custom_spinner_white);
            spinnerFilter.setAdapter(filterAdapter);
        } catch (Exception e) {
            Log.e(TAG, "Error setting up filter spinner: " + e.getMessage(), e);
        }
    }

    private void setupClickListeners() {
        btnBackHistory.setOnClickListener(v -> finish());

        btnApplyFilter.setOnClickListener(v -> {
            currentFilter = spinnerFilter.getSelectedItem().toString();
            if (currentFilter.equals("All")) {
                loadHistory(null);
            } else {
                loadHistory(currentFilter);
            }
        });

        btnClearFilter.setOnClickListener(v -> {
            spinnerFilter.setSelection(0);
            currentFilter = "All";
            loadHistory(null);
        });

        lvHistory.setOnItemClickListener((adapterView, view, position, id) -> {
            if (position < transactionIds.size()) {
                int transactionId = transactionIds.get(position);
                int targetUserId = targetUserIds.get(position);
                String originalMessage = messages.get(position);
                String actionType = actionTypes.get(position);
                int inquiryId = inquiryIds.get(position);
                String documentName = documentNames.get(position);

                // Only show re-response option for specific action types
                if (RE_RESPONDABLE_ACTIONS.contains(actionType)) {
                    showReResponseDialog(transactionId, targetUserId, originalMessage, actionType, inquiryId, documentName);
                } else {
                    showTransactionDetailsDialog(transactionId, actionType, originalMessage);
                }
            }
        });
    }

    private void loadHistory(String filter) {
        historyList.clear();
        transactionIds.clear();
        targetUserIds.clear();
        messages.clear();
        actionTypes.clear();
        inquiryIds.clear();
        documentNames.clear();

        SQLiteDatabase db = dbHelper.getReadableDatabase();

        try {
            String query;
            String[] selectionArgs;

            if (filter == null || filter.equals("All")) {
                query = "SELECT t.id, t.user_id, t.description, t.timestamp, t.action_type, " +
                        "COALESCE(t.inquiry_id, -1) as inquiry_id " +
                        "FROM transactions t " +
                        "WHERE t.user_id=? ORDER BY t.timestamp DESC";
                selectionArgs = new String[]{String.valueOf(facultyId)};
            } else {
                query = "SELECT t.id, t.user_id, t.description, t.timestamp, t.action_type, " +
                        "COALESCE(t.inquiry_id, -1) as inquiry_id " +
                        "FROM transactions t " +
                        "WHERE t.user_id=? AND t.action_type=? ORDER BY t.timestamp DESC";
                selectionArgs = new String[]{String.valueOf(facultyId), filter};
            }

            Cursor cursor = db.rawQuery(query, selectionArgs);

            if (cursor.moveToFirst()) {
                do {
                    int id = cursor.getInt(0);
                    int userId = cursor.getInt(1);
                    String msg = cursor.getString(2);
                    long time = cursor.getLong(3);
                    String actionType = cursor.getString(4);
                    int inquiryId = cursor.getInt(5);

                    // Extract target user ID and document name based on action type
                    int targetUserId = extractTargetUserId(msg, actionType, inquiryId);
                    String documentName = extractDocumentName(msg, actionType);

                    String formattedTime = formatTime(time);
                    String icon = getActionIcon(actionType);
                    String reRespondable = RE_RESPONDABLE_ACTIONS.contains(actionType) ? " 🔄" : "";

                    String displayText = icon + " " + formattedTime + reRespondable + "\n";
                    if (actionType != null && !actionType.isEmpty()) {
                        displayText += actionType + ": ";
                    }
                    displayText += msg;

                    historyList.add(displayText);
                    transactionIds.add(id);
                    targetUserIds.add(targetUserId);
                    messages.add(msg);
                    actionTypes.add(actionType != null ? actionType : "");
                    inquiryIds.add(inquiryId);
                    documentNames.add(documentName);

                } while (cursor.moveToNext());
            } else {
                historyList.add("No transaction history found" +
                        (filter != null && !filter.equals("All") ? " for " + filter : ""));
                transactionIds.add(-1);
                targetUserIds.add(-1);
                messages.add("");
                actionTypes.add("");
                inquiryIds.add(-1);
                documentNames.add("");
            }

            cursor.close();

            adapter = new ArrayAdapter<>(this, R.layout.custom_list_item, historyList);
            lvHistory.setAdapter(adapter);

        } catch (Exception e) {
            Log.e(TAG, "Error loading history: " + e.getMessage());
            Toast.makeText(this, "Error loading history", Toast.LENGTH_SHORT).show();
            historyList.add("Error loading transaction history");
            adapter = new ArrayAdapter<>(this, R.layout.custom_list_item, historyList);
            lvHistory.setAdapter(adapter);
        }
    }

    private int extractTargetUserId(String message, String actionType, int inquiryId) {
        if ("Faculty Inquiry Response".equals(actionType) && inquiryId != -1) {
            // Get student ID from inquiry
            try {
                SQLiteDatabase db = dbHelper.getReadableDatabase();
                Cursor cursor = db.rawQuery(
                        "SELECT student_id FROM faculty_inquiries WHERE id = ?",
                        new String[]{String.valueOf(inquiryId)}
                );
                if (cursor.moveToFirst()) {
                    int studentId = cursor.getInt(0);
                    cursor.close();
                    return studentId;
                }
                cursor.close();
            } catch (Exception e) {
                Log.e(TAG, "Error extracting student ID: " + e.getMessage());
            }
        }
        return -1; // Default if unable to extract
    }

    private String extractDocumentName(String message, String actionType) {
        if ("Document Status Update".equals(actionType)) {
            // Extract document name from message like "Your document 'DocName' has been approved"
            if (message.contains("document '") && message.contains("' has been")) {
                int start = message.indexOf("document '") + 10;
                int end = message.indexOf("' has been");
                if (start > 9 && end > start) {
                    return message.substring(start, end);
                }
            }
        }
        return "";
    }

    private String getActionIcon(String actionType) {
        if (actionType == null) return "📝";

        switch (actionType) {
            case "Faculty Inquiry Response":
                return "💬";
            case "Document Status Update":
                return "📋";
            case "Faculty Inquiry":
                return "❓";
            case "Document Submission":
                return "📄";
            default:
                return "📝";
        }
    }

    private String formatTime(long timestamp) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        } catch (Exception e) {
            return "Unknown time";
        }
    }

    private void showTransactionDetailsDialog(int transactionId, String actionType, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Transaction Details");

        String details = "Action Type: " + actionType + "\n\n" +
                "Description: " + message + "\n\n" +
                "Transaction ID: " + transactionId;

        if (!RE_RESPONDABLE_ACTIONS.contains(actionType)) {
            details += "\n\nNote: This transaction type cannot be re-responded to.";
        }

        builder.setMessage(details);
        builder.setPositiveButton("Close", null);
        builder.show();
    }

    private void showReResponseDialog(int transactionId, int targetUserId, String originalMessage,
                                      String actionType, int inquiryId, String documentName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        if ("Faculty Inquiry Response".equals(actionType)) {
            builder.setTitle("Re-Respond to Inquiry");
            builder.setMessage("Change your response to this inquiry:");

            builder.setPositiveButton("Available", (dialog, which) ->
                    reRespondToInquiry(inquiryId, targetUserId, "Available"));

            builder.setNegativeButton("Unavailable", (dialog, which) ->
                    reRespondToInquiry(inquiryId, targetUserId, "Unavailable"));

        } else if ("Document Status Update".equals(actionType)) {
            builder.setTitle("Re-Respond to Document");
            builder.setMessage("Change the document status for: " + documentName);

            builder.setPositiveButton("Approved", (dialog, which) ->
                    reRespondToDocument(documentName, targetUserId, "Approved"));

            builder.setNegativeButton("Rejected", (dialog, which) ->
                    reRespondToDocument(documentName, targetUserId, "Rejected"));
        }

        builder.setNeutralButton("Cancel", null);
        builder.show();
    }

    private void reRespondToInquiry(int inquiryId, int studentId, String newResponse) {
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            db.beginTransaction();

            // Update inquiry status
            ContentValues inquiryUpdate = new ContentValues();
            inquiryUpdate.put("status", newResponse);
            inquiryUpdate.put("response_time", System.currentTimeMillis());

            int updateResult = db.update("faculty_inquiries", inquiryUpdate, "id=?",
                    new String[]{String.valueOf(inquiryId)});

            if (updateResult > 0) {
                // Get inquiry subject for better notification
                String subject = "";
                Cursor cursor = db.rawQuery("SELECT subject FROM faculty_inquiries WHERE id=?",
                        new String[]{String.valueOf(inquiryId)});
                if (cursor.moveToFirst()) {
                    subject = cursor.getString(0);
                }
                cursor.close();

                // Create new notification for student
                ContentValues studentNotification = new ContentValues();
                studentNotification.put("user_id", studentId);
                studentNotification.put("action_type", "Faculty Response");
                studentNotification.put("description", "Faculty updated response to '" + newResponse +
                        "' for your inquiry: " + subject);
                studentNotification.put("timestamp", System.currentTimeMillis());
                studentNotification.put("read_status", 0);

                db.insert("transactions", null, studentNotification);

                // Record faculty re-response in faculty's history
                ContentValues facultyTransaction = new ContentValues();
                facultyTransaction.put("user_id", facultyId);
                facultyTransaction.put("action_type", "Faculty Inquiry Response");
                facultyTransaction.put("description", "Updated response to '" + newResponse +
                        "' for inquiry: " + subject);
                facultyTransaction.put("timestamp", System.currentTimeMillis());

                db.insert("transactions", null, facultyTransaction);

                db.setTransactionSuccessful();
                Toast.makeText(this, "Response updated to " + newResponse, Toast.LENGTH_SHORT).show();
                loadHistory(currentFilter.equals("All") ? null : currentFilter);

            } else {
                Toast.makeText(this, "Error updating response", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error re-responding to inquiry: " + e.getMessage(), e);
            Toast.makeText(this, "Error updating response", Toast.LENGTH_SHORT).show();
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

    private void reRespondToDocument(String documentName, int studentId, String newStatus) {
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            db.beginTransaction();

            // Update document status
            ContentValues docUpdate = new ContentValues();
            docUpdate.put("status", newStatus);

            int updateResult = db.update("documents", docUpdate, "name=?",
                    new String[]{documentName});

            if (updateResult > 0) {
                // Create new notification for student
                ContentValues studentNotification = new ContentValues();
                studentNotification.put("user_id", studentId);
                studentNotification.put("action_type", "Document Status Update");
                studentNotification.put("description", "Document status updated to '" + newStatus +
                        "' for: " + documentName);
                studentNotification.put("timestamp", System.currentTimeMillis());
                studentNotification.put("read_status", 0);

                db.insert("transactions", null, studentNotification);

                // Record faculty re-response in faculty's history
                ContentValues facultyTransaction = new ContentValues();
                facultyTransaction.put("user_id", facultyId);
                facultyTransaction.put("action_type", "Document Status Update");
                facultyTransaction.put("description", "Updated document '" + documentName +
                        "' status to " + newStatus);
                facultyTransaction.put("timestamp", System.currentTimeMillis());

                db.insert("transactions", null, facultyTransaction);

                db.setTransactionSuccessful();
                Toast.makeText(this, "Document status updated to " + newStatus, Toast.LENGTH_SHORT).show();
                loadHistory(currentFilter.equals("All") ? null : currentFilter);

            } else {
                Toast.makeText(this, "Error updating document status", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error re-responding to document: " + e.getMessage(), e);
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

    @Override
    protected void onResume() {
        super.onResume();
        loadHistory(currentFilter.equals("All") ? null : currentFilter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}