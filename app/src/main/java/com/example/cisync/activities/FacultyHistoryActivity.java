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

                // Show transaction details dialog for all transactions
                showTransactionDetailsDialog(transactionId, actionType, originalMessage);
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
        } else if ("Document Status Update".equals(actionType)) {
            // Try to extract document name and find student ID
            String docName = extractDocumentName(message, actionType);
            if (!docName.isEmpty()) {
                try {
                    SQLiteDatabase db = dbHelper.getReadableDatabase();
                    Cursor cursor = db.rawQuery(
                            "SELECT student_id FROM documents WHERE name = ?",
                            new String[]{docName}
                    );
                    if (cursor.moveToFirst()) {
                        int studentId = cursor.getInt(0);
                        cursor.close();
                        return studentId;
                    }
                    cursor.close();
                } catch (Exception e) {
                    Log.e(TAG, "Error extracting student ID from document: " + e.getMessage());
                }
            }
        }
        return -1; // Default if unable to extract
    }

    private String extractDocumentName(String message, String actionType) {
        if ("Document Status Update".equals(actionType)) {
            // Extract document name from messages like "Updated document 'DocName' status to Approved"
            if (message.contains("document '") && message.contains("' status to")) {
                int start = message.indexOf("document '") + 10;
                int end = message.indexOf("' status to");
                if (start > 9 && end > start) {
                    return message.substring(start, end);
                }
            }
            // Extract document name from messages like "Your document 'DocName' has been approved"
            else if (message.contains("document '") && message.contains("' has been")) {
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
        // Create custom dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_faculty_transaction, null);
        builder.setView(dialogView);

        // Get references to views
        TextView tvTransactionId = dialogView.findViewById(R.id.tvTransactionId);
        TextView tvTransactionActionType = dialogView.findViewById(R.id.tvTransactionActionType);
        TextView tvTransactionTimestamp = dialogView.findViewById(R.id.tvTransactionTimestamp);
        TextView tvReResponseStatus = dialogView.findViewById(R.id.tvReResponseStatus);
        TextView tvTransactionDescription = dialogView.findViewById(R.id.tvTransactionDescription);
        TextView tvAdditionalInfo = dialogView.findViewById(R.id.tvAdditionalInfo);
        LinearLayout llAdditionalInfo = dialogView.findViewById(R.id.llAdditionalInfo);
        LinearLayout llReResponseSection = dialogView.findViewById(R.id.llReResponseSection);
        TextView tvReResponseMessage = dialogView.findViewById(R.id.tvReResponseMessage);
        Button btnReResponseOption1 = dialogView.findViewById(R.id.btnReResponseOption1);
        Button btnReResponseOption2 = dialogView.findViewById(R.id.btnReResponseOption2);
        View vReResponseIndicator = dialogView.findViewById(R.id.vReResponseIndicator);
        Button btnClose = dialogView.findViewById(R.id.btnTransactionClose);

        // Set transaction data
        tvTransactionId.setText(String.valueOf(transactionId));
        tvTransactionActionType.setText(actionType != null ? actionType : "Unknown");
        tvTransactionDescription.setText(message != null ? message : "No description available");

        // Get transaction details from database
        TransactionDetails details = getTransactionDetails(transactionId);

        // Set timestamp
        if (details.timestamp != -1) {
            String formattedTime = formatTime(details.timestamp);
            tvTransactionTimestamp.setText(formattedTime);
        } else {
            tvTransactionTimestamp.setText("Unknown");
        }

        // Set re-response status and show/hide re-response section
        boolean isReRespondable = RE_RESPONDABLE_ACTIONS.contains(actionType);
        if (isReRespondable) {
            tvReResponseStatus.setText("Available");
            tvReResponseStatus.setTextColor(getResources().getColor(android.R.color.holo_green_light));
            vReResponseIndicator.setBackgroundResource(R.drawable.status_indicator_active);

            // Show re-response section
            llReResponseSection.setVisibility(View.VISIBLE);

            // Configure re-response buttons based on action type
            configureReResponseButtons(actionType, btnReResponseOption1, btnReResponseOption2,
                    tvReResponseMessage, details);

            // Create and show dialog first to get reference
            AlertDialog dialog = builder.create();
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

            // Set button click listeners with dialog reference
            setupReResponseListeners(dialogView, transactionId, actionType, details, dialog);

            // Set close button listener
            btnClose.setOnClickListener(v -> dialog.dismiss());

            dialog.show();

        } else {
            tvReResponseStatus.setText("Not Available");
            tvReResponseStatus.setTextColor(getResources().getColor(android.R.color.holo_red_light));
            vReResponseIndicator.setBackgroundResource(R.drawable.status_indicator_active);
            llReResponseSection.setVisibility(View.GONE);

            // Create and show dialog
            AlertDialog dialog = builder.create();
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

            // Set close button listener
            btnClose.setOnClickListener(v -> dialog.dismiss());

            dialog.show();
        }

        // Set additional information if needed
        String additionalInfo = getAdditionalTransactionInfo(transactionId, actionType);
        if (additionalInfo != null && !additionalInfo.isEmpty()) {
            llAdditionalInfo.setVisibility(View.VISIBLE);
            tvAdditionalInfo.setText(additionalInfo);
        } else {
            llAdditionalInfo.setVisibility(View.GONE);
        }
    }

    // Helper class to store transaction details
    private static class TransactionDetails {
        int targetUserId = -1;
        int inquiryId = -1;
        String documentName = "";
        long timestamp = -1;
        String originalMessage = "";
    }

    // Get comprehensive transaction details
    private TransactionDetails getTransactionDetails(int transactionId) {
        TransactionDetails details = new TransactionDetails();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        try {
            // First get basic transaction information
            Cursor cursor = db.rawQuery(
                    "SELECT t.timestamp, t.description, t.action_type, COALESCE(t.inquiry_id, -1) as inquiry_id " +
                            "FROM transactions t WHERE t.id = ?",
                    new String[]{String.valueOf(transactionId)}
            );

            if (cursor.moveToFirst()) {
                details.timestamp = cursor.getLong(0);
                details.originalMessage = cursor.getString(1);
                String actionType = cursor.getString(2);
                details.inquiryId = cursor.getInt(3);

                // Extract document name if it's a document status update
                if ("Document Status Update".equals(actionType)) {
                    if (details.originalMessage.contains("document '") && details.originalMessage.contains("' status to")) {
                        int start = details.originalMessage.indexOf("document '") + 10;
                        int end = details.originalMessage.indexOf("' status to");
                        if (start > 9 && end > start) {
                            details.documentName = details.originalMessage.substring(start, end);
                        }
                    } else if (details.originalMessage.contains("document '") && details.originalMessage.contains("' has been")) {
                        int start = details.originalMessage.indexOf("document '") + 10;
                        int end = details.originalMessage.indexOf("' has been");
                        if (start > 9 && end > start) {
                            details.documentName = details.originalMessage.substring(start, end);
                        }
                    }

                    // If we have a document name, try to get student ID
                    if (!details.documentName.isEmpty()) {
                        cursor.close();
                        cursor = db.rawQuery(
                                "SELECT student_id FROM documents WHERE name = ?",
                                new String[]{details.documentName}
                        );
                        if (cursor.moveToFirst()) {
                            details.targetUserId = cursor.getInt(0);
                        }
                    }
                }
                // For faculty inquiry responses, get the student ID from the inquiry
                else if (("Faculty Inquiry Response".equals(actionType) || "Faculty Response".equals(actionType))
                        && details.inquiryId != -1) {
                    cursor.close();
                    cursor = db.rawQuery(
                            "SELECT student_id FROM faculty_inquiries WHERE id = ?",
                            new String[]{String.valueOf(details.inquiryId)}
                    );
                    if (cursor.moveToFirst()) {
                        details.targetUserId = cursor.getInt(0);
                    }
                }
            }

            cursor.close();
        } catch (Exception e) {
            Log.e(TAG, "Error getting transaction details: " + e.getMessage());
        }

        return details;
    }

    // Configure re-response buttons based on action type
    private void configureReResponseButtons(String actionType, Button btn1, Button btn2,
                                            TextView messageView, TransactionDetails details) {
        if ("Faculty Inquiry Response".equals(actionType)) {
            messageView.setText("Change your response to this inquiry:");
            btn1.setText("AVAILABLE");
            btn2.setText("UNAVAILABLE");

        } else if ("Document Status Update".equals(actionType)) {
            messageView.setText("Change the document status for: " + details.documentName);
            btn1.setText("APPROVED");
            btn2.setText("REJECTED");
        }
    }

    // Setup re-response button listeners
    private void setupReResponseListeners(View dialogView, int transactionId, String actionType,
                                          TransactionDetails details, AlertDialog dialog) {
        Button btnOption1 = dialogView.findViewById(R.id.btnReResponseOption1);
        Button btnOption2 = dialogView.findViewById(R.id.btnReResponseOption2);

        if ("Faculty Inquiry Response".equals(actionType)) {
            btnOption1.setOnClickListener(v -> {
                showReResponseConfirmation("Available", () -> {
                    reRespondToInquiry(details.inquiryId, details.targetUserId, "Available");
                    dialog.dismiss();
                });
            });

            btnOption2.setOnClickListener(v -> {
                showReResponseConfirmation("Unavailable", () -> {
                    reRespondToInquiry(details.inquiryId, details.targetUserId, "Unavailable");
                    dialog.dismiss();
                });
            });

        } else if ("Document Status Update".equals(actionType)) {
            btnOption1.setOnClickListener(v -> {
                showReResponseConfirmation("Approved", () -> {
                    reRespondToDocument(details.documentName, details.targetUserId, "Approved");
                    dialog.dismiss();
                });
            });

            btnOption2.setOnClickListener(v -> {
                showReResponseConfirmation("Rejected", () -> {
                    reRespondToDocument(details.documentName, details.targetUserId, "Rejected");
                    dialog.dismiss();
                });
            });
        }
    }

    // Show confirmation dialog before re-responding
    private void showReResponseConfirmation(String newResponse, Runnable onConfirm) {
        AlertDialog.Builder confirmBuilder = new AlertDialog.Builder(this);
        confirmBuilder.setTitle("Confirm Re-Response");
        confirmBuilder.setMessage("Are you sure you want to change your response to: " + newResponse + "?");

        confirmBuilder.setPositiveButton("Confirm", (dialog, which) -> {
            onConfirm.run();
        });

        confirmBuilder.setNegativeButton("Cancel", null);
        confirmBuilder.show();
    }

    // Updated reRespondToInquiry method with better error handling
    private void reRespondToInquiry(int inquiryId, int studentId, String newResponse) {
        if (inquiryId == -1 || studentId == -1) {
            Toast.makeText(this, "Error: Invalid inquiry or student ID", Toast.LENGTH_SHORT).show();
            return;
        }

        SQLiteDatabase db = null;

        try {
            db = dbHelper.getWritableDatabase();
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
                studentNotification.put("read_status", 0); // Mark as unread
                studentNotification.put("inquiry_id", inquiryId); // Link to original inquiry

                long studentNotifyResult = db.insert("transactions", null, studentNotification);

                // Record faculty re-response in faculty's history
                ContentValues facultyTransaction = new ContentValues();
                facultyTransaction.put("user_id", facultyId);
                facultyTransaction.put("action_type", "Faculty Inquiry Response");
                facultyTransaction.put("description", "Updated response to '" + newResponse +
                        "' for inquiry: " + subject);
                facultyTransaction.put("timestamp", System.currentTimeMillis());
                facultyTransaction.put("inquiry_id", inquiryId);

                long facultyTransResult = db.insert("transactions", null, facultyTransaction);

                if (studentNotifyResult != -1 && facultyTransResult != -1) {
                    db.setTransactionSuccessful();
                    Toast.makeText(this, "Response updated to " + newResponse, Toast.LENGTH_SHORT).show();
                    loadHistory(currentFilter.equals("All") ? null : currentFilter);
                } else {
                    Toast.makeText(this, "Error recording transaction", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Error updating response: Inquiry not found", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error re-responding to inquiry: " + e.getMessage(), e);
            Toast.makeText(this, "Error updating response: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            if (db != null) {
                if (db.inTransaction()) {
                    db.endTransaction();
                }
            }
        }
    }

    // Updated reRespondToDocument method with better error handling
    private void reRespondToDocument(String documentName, int studentId, String newStatus) {
        if (documentName == null || documentName.isEmpty() || studentId == -1) {
            Toast.makeText(this, "Error: Invalid document or student ID", Toast.LENGTH_SHORT).show();
            return;
        }

        SQLiteDatabase db = null;

        try {
            db = dbHelper.getWritableDatabase();
            db.beginTransaction();

            // Update document status
            ContentValues docUpdate = new ContentValues();
            docUpdate.put("status", newStatus);

            int updateResult = db.update("documents", docUpdate, "name=? AND student_id=?",
                    new String[]{documentName, String.valueOf(studentId)});

            if (updateResult > 0) {
                // Create new notification for student
                ContentValues studentNotification = new ContentValues();
                studentNotification.put("user_id", studentId);
                studentNotification.put("action_type", "Document Status Update");
                studentNotification.put("description", "Your document '" + documentName +
                        "' has been " + newStatus.toLowerCase());
                studentNotification.put("timestamp", System.currentTimeMillis());
                studentNotification.put("read_status", 0); // Mark as unread

                long studentNotifyResult = db.insert("transactions", null, studentNotification);

                // Record faculty re-response in faculty's history
                ContentValues facultyTransaction = new ContentValues();
                facultyTransaction.put("user_id", facultyId);
                facultyTransaction.put("action_type", "Document Status Update");
                facultyTransaction.put("description", "Updated document '" + documentName +
                        "' status to " + newStatus);
                facultyTransaction.put("timestamp", System.currentTimeMillis());

                long facultyTransResult = db.insert("transactions", null, facultyTransaction);

                if (studentNotifyResult != -1 && facultyTransResult != -1) {
                    db.setTransactionSuccessful();
                    Toast.makeText(this, "Document status updated to " + newStatus, Toast.LENGTH_SHORT).show();
                    loadHistory(currentFilter.equals("All") ? null : currentFilter);
                } else {
                    Toast.makeText(this, "Error recording transaction", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Error updating document status: Document not found", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error re-responding to document: " + e.getMessage(), e);
            Toast.makeText(this, "Error updating document status: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            if (db != null) {
                if (db.inTransaction()) {
                    db.endTransaction();
                }
            }
        }
    }

    // Helper method to get additional transaction information
    private String getAdditionalTransactionInfo(int transactionId, String actionType) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        StringBuilder additionalInfo = new StringBuilder();

        try {
            if ("Faculty Inquiry Response".equals(actionType)) {
                // Get inquiry details
                Cursor cursor = db.rawQuery(
                        "SELECT fi.subject, fi.description, u.name as student_name, fi.status " +
                                "FROM transactions t " +
                                "JOIN faculty_inquiries fi ON t.inquiry_id = fi.id " +
                                "JOIN users u ON fi.student_id = u.id " +
                                "WHERE t.id = ?",
                        new String[]{String.valueOf(transactionId)}
                );

                if (cursor.moveToFirst()) {
                    String subject = cursor.getString(0);
                    String inquiryMessage = cursor.getString(1);
                    String studentName = cursor.getString(2);
                    String currentStatus = cursor.getString(3);

                    additionalInfo.append("Inquiry Subject: ").append(subject).append("\n");
                    additionalInfo.append("Student: ").append(studentName).append("\n");
                    additionalInfo.append("Current Status: ").append(currentStatus).append("\n");
                    additionalInfo.append("Original Inquiry: ").append(inquiryMessage);
                }
                cursor.close();

            } else if ("Document Status Update".equals(actionType)) {
                // Get document details
                Cursor cursor = db.rawQuery(
                        "SELECT d.name, d.type, d.status, u.name as student_name " +
                                "FROM transactions t " +
                                "JOIN documents d ON t.description LIKE '%' || d.name || '%' " +
                                "JOIN users u ON d.student_id = u.id " +
                                "WHERE t.id = ? LIMIT 1",
                        new String[]{String.valueOf(transactionId)}
                );

                if (cursor.moveToFirst()) {
                    String docName = cursor.getString(0);
                    String docType = cursor.getString(1);
                    String currentStatus = cursor.getString(2);
                    String studentName = cursor.getString(3);

                    additionalInfo.append("Document: ").append(docName).append("\n");
                    additionalInfo.append("Type: ").append(docType != null ? docType : "Unknown").append("\n");
                    additionalInfo.append("Current Status: ").append(currentStatus).append("\n");
                    additionalInfo.append("Student: ").append(studentName);
                }
                cursor.close();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error getting additional info: " + e.getMessage());
            return null;
        }

        return additionalInfo.length() > 0 ? additionalInfo.toString() : null;
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