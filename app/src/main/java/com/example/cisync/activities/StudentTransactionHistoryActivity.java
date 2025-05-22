package com.example.cisync.activities;

import android.app.Activity;
import android.app.AlertDialog;
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

public class StudentTransactionHistoryActivity extends Activity {

    private static final String TAG = "StudentTransactionHistory";
    ListView lvTransactionHistory;
    TextView tvNoTransactions;
    Spinner spinnerTransactionFilter;
    Button btnApplyFilter, btnClearFilter;
    ImageView btnBack;

    DBHelper dbHelper;
    ArrayList<String> transactionList = new ArrayList<>();
    ArrayList<TransactionData> transactionHistoryList = new ArrayList<>();
    ArrayAdapter<String> adapter;
    String currentFilter = "All"; // Default filter
    int studentId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_transaction_history);

        // Get student ID from intent
        studentId = getIntent().getIntExtra("studentId", -1);

        if (studentId == -1) {
            Log.e(TAG, "No valid student ID received");
            Toast.makeText(this, "Error: No valid student ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Log.d(TAG, "Student ID: " + studentId);

        // Initialize views
        initializeViews();

        // Set up filter spinner
        setupFilterSpinner();

        // Set up click listeners
        setupClickListeners();

        // Load initial data
        loadTransactionHistory(null);
    }

    private void initializeViews() {
        try {
            lvTransactionHistory = findViewById(R.id.lvTransactionHistory);
            tvNoTransactions = findViewById(R.id.tvNoTransactions);
            spinnerTransactionFilter = findViewById(R.id.spinnerTransactionFilter);
            btnApplyFilter = findViewById(R.id.btnApplyFilter);
            btnClearFilter = findViewById(R.id.btnClearFilter);
            btnBack = findViewById(R.id.btnBack);
            dbHelper = new DBHelper(this);
        } catch (Exception e) {
            Log.e(TAG, "Error initializing views: " + e.getMessage(), e);
            Toast.makeText(this, "Error initializing views", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupFilterSpinner() {
        try {
            // Create filter options specific to student activities
            ArrayList<String> filterOptions = new ArrayList<>();
            filterOptions.add("All");
            filterOptions.add("Faculty Inquiry");
            filterOptions.add("Document Submission");
            filterOptions.add("Document Status Update");
            filterOptions.add("Notice Posted");
            filterOptions.add("Accountability Posted"); // Added for Accountability Management
            filterOptions.add("Accountability Status Update"); // Added for Accountability Management
            filterOptions.add("Accountability Deleted"); // Added for Accountability Management
            filterOptions.add("User Update");
            filterOptions.add("Registration");

            // Set up spinner adapter
            ArrayAdapter<String> filterAdapter = new ArrayAdapter<>(
                    this, R.layout.custom_spinner_white, filterOptions);
            filterAdapter.setDropDownViewResource(R.layout.custom_spinner_white);
            spinnerTransactionFilter.setAdapter(filterAdapter);
        } catch (Exception e) {
            Log.e(TAG, "Error setting up filter spinner: " + e.getMessage(), e);
        }
    }

    private void setupClickListeners() {
        try {
            // Back button
            btnBack.setOnClickListener(v -> finish());

            // Apply filter button
            btnApplyFilter.setOnClickListener(v -> {
                currentFilter = spinnerTransactionFilter.getSelectedItem().toString();
                if (currentFilter.equals("All")) {
                    loadTransactionHistory(null);
                } else {
                    loadTransactionHistory(currentFilter);
                }
            });

            // Clear filter button
            btnClearFilter.setOnClickListener(v -> {
                spinnerTransactionFilter.setSelection(0); // Set to "All"
                currentFilter = "All";
                loadTransactionHistory(null);
            });

            // List item click listener for viewing details
            lvTransactionHistory.setOnItemClickListener((parent, view, position, id) -> {
                if (position < transactionHistoryList.size()) {
                    TransactionData transaction = transactionHistoryList.get(position);
                    showTransactionDetailsDialog(transaction);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error setting up click listeners: " + e.getMessage(), e);
        }
    }

    private void loadTransactionHistory(String filter) {
        transactionList.clear();
        transactionHistoryList.clear();

        SQLiteDatabase db = dbHelper.getReadableDatabase();

        try {
            String query;
            String[] selectionArgs;

            if (filter == null || filter.equals("All")) {
                // No filter - get all transactions for this student
                query = "SELECT id, action_type, description, timestamp FROM transactions " +
                        "WHERE user_id = ? ORDER BY timestamp DESC";
                selectionArgs = new String[]{String.valueOf(studentId)};
            } else {
                // Apply filter by action_type for this student
                query = "SELECT id, action_type, description, timestamp FROM transactions " +
                        "WHERE user_id = ? AND action_type LIKE ? ORDER BY timestamp DESC";
                selectionArgs = new String[]{String.valueOf(studentId), "%" + filter + "%"};
            }

            Cursor cursor = db.rawQuery(query, selectionArgs);

            if (cursor.moveToFirst() && cursor.getCount() > 0) {
                // Hide empty message, show list
                tvNoTransactions.setVisibility(View.GONE);
                lvTransactionHistory.setVisibility(View.VISIBLE);

                do {
                    int id = cursor.getInt(0);
                    String actionType = cursor.getString(1);
                    String description = cursor.getString(2);
                    long timestamp = cursor.getLong(3);

                    // Create transaction object
                    TransactionData transaction = new TransactionData(
                            id, studentId, actionType, description, timestamp);
                    transactionHistoryList.add(transaction);

                    // Format for display
                    String formattedDate = formatTimestamp(timestamp);

                    // Create display text with icons for different transaction types
                    String displayText = getTransactionIcon(actionType) + " " + formattedDate + "\n" +
                            (actionType != null ? actionType : "Unknown action") + ":\n" +
                            description;

                    transactionList.add(displayText);
                } while (cursor.moveToNext());

            } else {
                // No transactions found - show message
                tvNoTransactions.setVisibility(View.VISIBLE);
                lvTransactionHistory.setVisibility(View.GONE);

                String filterText = (filter != null && !filter.equals("All")) ? " for " + filter : "";
                tvNoTransactions.setText("No transaction history found" + filterText);
            }

            cursor.close();

            // Update adapter
            adapter = new ArrayAdapter<>(this, R.layout.custom_list_item, transactionList);
            lvTransactionHistory.setAdapter(adapter);

        } catch (Exception e) {
            Log.e(TAG, "Error loading transaction history: " + e.getMessage(), e);
            Toast.makeText(this, "Error loading transaction history: " + e.getMessage(), Toast.LENGTH_SHORT).show();

            // Show error state
            tvNoTransactions.setVisibility(View.VISIBLE);
            tvNoTransactions.setText("Error loading transaction history");
            lvTransactionHistory.setVisibility(View.GONE);
        }
    }

    private String getTransactionIcon(String actionType) {
        if (actionType == null) return "📝";

        switch (actionType) {
            case "Faculty Inquiry":
                return "❓";
            case "Document Submission":
                return "📄";
            case "Document Status Update":
                return "📋";
            case "Notice Posted":
                return "📢";
            case "Accountability Posted":
                return "💰";
            case "Accountability Status Update":
                return "💳";
            case "Accountability Deleted":
                return "🗑️";
            case "Registration":
                return "👤";
            case "User Update":
                return "✏️";
            default:
                return "📝";
        }
    }

    private String formatTimestamp(long timestamp) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        } catch (Exception e) {
            return "Unknown date";
        }
    }

    private void showTransactionDetailsDialog(TransactionData transaction) {
        try {
            // Create custom dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            // Inflate custom layout
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_student_transaction_details, null);
            builder.setView(dialogView);

            // Create dialog
            AlertDialog dialog = builder.create();

            // Make dialog background transparent to show custom background
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }

            // Find views in custom layout
            TextView tvTransactionId = dialogView.findViewById(R.id.tvDialogTransactionId);
            TextView tvActionType = dialogView.findViewById(R.id.tvDialogActionType);
            TextView tvTransactionTime = dialogView.findViewById(R.id.tvDialogTransactionTime);
            TextView tvDescription = dialogView.findViewById(R.id.tvDialogDescription);
            TextView tvTransactionIcon = dialogView.findViewById(R.id.tvTransactionIcon);
            TextView tvTransactionCategory = dialogView.findViewById(R.id.tvTransactionCategory);
            TextView tvTransactionSubcategory = dialogView.findViewById(R.id.tvTransactionSubcategory);
            View vStatusIndicator = dialogView.findViewById(R.id.vTransactionStatusIndicator);
            LinearLayout llAdditionalDetails = dialogView.findViewById(R.id.llAdditionalDetails);
            LinearLayout llAdditionalDetailsContent = dialogView.findViewById(R.id.llAdditionalDetailsContent);
            TextView tvAdditionalDetailsTitle = dialogView.findViewById(R.id.tvAdditionalDetailsTitle);
            Button btnDialogClose = dialogView.findViewById(R.id.btnDialogClose);

            // Set basic transaction information
            tvTransactionId.setText(String.valueOf(transaction.getId()));
            tvActionType.setText(transaction.getActionType());
            tvTransactionTime.setText(formatTimestamp(transaction.getTimestamp()));
            tvDescription.setText(transaction.getDescription());

            // Set transaction-specific details
            String actionType = transaction.getActionType();
            setStudentTransactionTypeDetails(transaction, actionType, tvTransactionIcon, tvTransactionCategory,
                    tvTransactionSubcategory, tvActionType, vStatusIndicator);

            // Add additional details if available
            String additionalInfo = getStudentAdditionalTransactionDetails(transaction);
            if (!additionalInfo.isEmpty()) {
                llAdditionalDetails.setVisibility(View.VISIBLE);
                populateStudentAdditionalDetails(llAdditionalDetailsContent, additionalInfo, actionType, tvAdditionalDetailsTitle);
            } else {
                llAdditionalDetails.setVisibility(View.GONE);
            }

            // Set click listener for close button
            btnDialogClose.setOnClickListener(v -> dialog.dismiss());

            // Show dialog
            dialog.show();

        } catch (Exception e) {
            Log.e(TAG, "Error showing custom transaction details dialog: " + e.getMessage(), e);

            // Fallback to simple dialog if custom dialog fails
            showFallbackStudentTransactionDialog(transaction);
        }
    }

    private void setStudentTransactionTypeDetails(TransactionData transaction, String actionType, TextView tvIcon, TextView tvCategory,
                                                  TextView tvSubcategory, TextView tvActionType, View vStatusIndicator) {
        try {
            switch (actionType) {
                case "Faculty Inquiry":
                    tvIcon.setText("❓");
                    tvCategory.setText("Communication");
                    tvSubcategory.setText("Inquiry submitted to faculty");
                    tvActionType.setBackgroundColor(getResources().getColor(R.color.inquiry_action_color));
                    vStatusIndicator.setBackgroundResource(R.drawable.status_indicator_active);
                    break;

                case "Document Submission":
                    tvIcon.setText("📄");
                    tvCategory.setText("Document Management");
                    tvSubcategory.setText("Document submitted for review");
                    tvActionType.setBackgroundColor(getResources().getColor(R.color.document_action_color));
                    vStatusIndicator.setBackgroundResource(R.drawable.status_indicator_active);
                    break;

                case "Document Status Update":
                    tvIcon.setText("📋");
                    tvCategory.setText("Document Management");
                    tvSubcategory.setText("Document status changed");
                    tvActionType.setBackgroundColor(getResources().getColor(R.color.document_action_color));
                    if (transaction.getDescription().toLowerCase().contains("approved")) {
                        vStatusIndicator.setBackgroundResource(R.drawable.document_icon);
                    } else if (transaction.getDescription().toLowerCase().contains("rejected")) {
                        vStatusIndicator.setBackgroundResource(R.drawable.notice_icon);
                    } else {
                        vStatusIndicator.setBackgroundResource(R.drawable.status_indicator_active);
                    }
                    break;

                case "Notice Posted":
                    tvIcon.setText("📢");
                    tvCategory.setText("Communication");
                    tvSubcategory.setText("Notice published successfully");
                    tvActionType.setBackgroundColor(getResources().getColor(R.color.notice_action_color));
                    vStatusIndicator.setBackgroundResource(R.drawable.status_indicator_active);
                    break;

                case "Accountability Posted":
                    tvIcon.setText("💰");
                    tvCategory.setText("Financial Management");
                    tvSubcategory.setText("Accountability added");
                    tvActionType.setBackgroundColor(getResources().getColor(R.color.accountability_action_color));
                    vStatusIndicator.setBackgroundResource(R.drawable.status_indicator_active);
                    break;

                case "Accountability Status Update":
                    tvIcon.setText("💳");
                    tvCategory.setText("Financial Management");
                    tvSubcategory.setText("Payment status updated");
                    tvActionType.setBackgroundColor(getResources().getColor(R.color.accountability_action_color));
                    if (transaction.getDescription().toLowerCase().contains("paid")) {
                        vStatusIndicator.setBackgroundResource(R.drawable.wallet);
                    } else {
                        vStatusIndicator.setBackgroundResource(R.drawable.notice_icon);
                    }
                    break;

                case "Accountability Deleted":
                    tvIcon.setText("🗑️");
                    tvCategory.setText("Financial Management");
                    tvSubcategory.setText("Accountability removed");
                    tvActionType.setBackgroundColor(getResources().getColor(R.color.delete_action_color));
                    vStatusIndicator.setBackgroundResource(R.drawable.status_indicator_ended);
                    break;

                case "Registration":
                    tvIcon.setText("👤");
                    tvCategory.setText("Account Management");
                    tvSubcategory.setText("Account registered");
                    tvActionType.setBackgroundColor(getResources().getColor(R.color.user_action_color));
                    vStatusIndicator.setBackgroundResource(R.drawable.ic_check);
                    break;

                case "User Update":
                    tvIcon.setText("✏️");
                    tvCategory.setText("Account Management");
                    tvSubcategory.setText("Profile information updated");
                    tvActionType.setBackgroundColor(getResources().getColor(R.color.user_action_color));
                    vStatusIndicator.setBackgroundResource(R.drawable.status_indicator_active);
                    break;

                default:
                    tvIcon.setText("📝");
                    tvCategory.setText("System");
                    tvSubcategory.setText("General activity");
                    tvActionType.setBackgroundColor(getResources().getColor(R.color.default_role_color));
                    vStatusIndicator.setBackgroundResource(R.drawable.status_indicator_active);
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting student transaction type details: " + e.getMessage(), e);
        }
    }

    private String getStudentAdditionalTransactionDetails(TransactionData transaction) {
        String additionalInfo = "";

        try {
            String actionType = transaction.getActionType();
            String description = transaction.getDescription();

            switch (actionType) {
                case "Faculty Inquiry":
                    additionalInfo = "Inquiry Type: Faculty Communication\n" +
                            "Status: Submitted for review\n" +
                            "Response: Pending faculty response";
                    break;

                case "Document Submission":
                    additionalInfo = getStudentDocumentSubmissionDetails(transaction);
                    break;

                case "Document Status Update":
                    additionalInfo = getStudentDocumentStatusDetails(transaction);
                    break;

                case "Notice Posted":
                    additionalInfo = getStudentNoticeDetails(transaction);
                    break;

                case "Accountability Posted":
                    additionalInfo = getStudentAccountabilityPostedDetails(transaction);
                    break;

                case "Accountability Status Update":
                    additionalInfo = getStudentAccountabilityStatusDetails(transaction);
                    break;

                case "Accountability Deleted":
                    additionalInfo = "Action: Accountability removed\n" +
                            "Status: Permanently deleted from system\n" +
                            "Impact: No longer applicable to your account";
                    break;

                case "Registration":
                    additionalInfo = "Account Type: Student Account\n" +
                            "Status: Successfully registered\n" +
                            "Access Level: Student privileges activated";
                    break;

                case "User Update":
                    additionalInfo = "Update Type: Profile information\n" +
                            "Status: Successfully updated\n" +
                            "Changes: Profile data modified";
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting student additional transaction details: " + e.getMessage(), e);
        }

        return additionalInfo;
    }

    private void populateStudentAdditionalDetails(LinearLayout container, String additionalInfo,
                                                  String actionType, TextView titleView) {
        try {
            container.removeAllViews();

            // Set appropriate title based on action type
            switch (actionType) {
                case "Faculty Inquiry":
                    titleView.setText("INQUIRY STATUS");
                    break;
                case "Document Submission":
                case "Document Status Update":
                    titleView.setText("DOCUMENT STATUS");
                    break;
                case "Notice Posted":
                    titleView.setText("NOTICE STATUS");
                    break;
                case "Accountability Posted":
                case "Accountability Status Update":
                case "Accountability Deleted":
                    titleView.setText("FINANCIAL STATUS");
                    break;
                case "Registration":
                case "User Update":
                    titleView.setText("ACCOUNT STATUS");
                    break;
                default:
                    titleView.setText("ACTIVITY STATUS");
                    break;
            }

            if (!additionalInfo.isEmpty()) {
                String[] lines = additionalInfo.split("\n");

                for (String line : lines) {
                    if (!line.trim().isEmpty()) {
                        if (line.contains(":")) {
                            // Create key-value pair layout
                            LinearLayout rowLayout = new LinearLayout(this);
                            rowLayout.setOrientation(LinearLayout.HORIZONTAL);
                            rowLayout.setLayoutParams(new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT));
                            rowLayout.setPadding(0, 0, 0, 8);

                            String[] parts = line.split(":", 2);

                            // Key TextView
                            TextView keyView = new TextView(this);
                            LinearLayout.LayoutParams keyParams = new LinearLayout.LayoutParams(
                                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
                            keyView.setLayoutParams(keyParams);
                            keyView.setText(parts[0].trim() + ":");
                            keyView.setTextColor(getResources().getColor(android.R.color.white));
                            keyView.setTextSize(12);
                            keyView.setTypeface(null, android.graphics.Typeface.BOLD);

                            // Value TextView
                            TextView valueView = new TextView(this);
                            LinearLayout.LayoutParams valueParams = new LinearLayout.LayoutParams(
                                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 2);
                            valueView.setLayoutParams(valueParams);
                            valueView.setText(parts.length > 1 ? parts[1].trim() : "");
                            valueView.setTextColor(getResources().getColor(android.R.color.white));
                            valueView.setTextSize(12);

                            rowLayout.addView(keyView);
                            rowLayout.addView(valueView);
                            container.addView(rowLayout);
                        } else {
                            // Create single line TextView
                            TextView textView = new TextView(this);
                            textView.setLayoutParams(new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT));
                            textView.setText(line);
                            textView.setTextColor(getResources().getColor(android.R.color.white));
                            textView.setTextSize(12);
                            textView.setPadding(0, 0, 0, 8);

                            container.addView(textView);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error populating student additional details: " + e.getMessage(), e);
        }
    }

    private void showFallbackStudentTransactionDialog(TransactionData transaction) {
        try {
            AlertDialog.Builder fallbackBuilder = new AlertDialog.Builder(this);
            fallbackBuilder.setTitle("Transaction Details");

            String message = "Date: " + formatTimestamp(transaction.getTimestamp()) + "\n\n" +
                    "Action: " + transaction.getActionType() + "\n\n" +
                    "Description: " + transaction.getDescription() + "\n\n" +
                    "Transaction ID: " + transaction.getId();

            // Add additional details
            String additionalInfo = getStudentAdditionalTransactionDetails(transaction);
            if (!additionalInfo.isEmpty()) {
                message += "\n\n" + additionalInfo;
            }

            fallbackBuilder.setMessage(message);
            fallbackBuilder.setPositiveButton("Close", null);
            fallbackBuilder.show();

            Toast.makeText(this, "Using fallback dialog due to error", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing fallback dialog: " + e.getMessage(), e);
            Toast.makeText(this, "Error displaying transaction details", Toast.LENGTH_SHORT).show();
        }
    }

    // Helper methods for getting specific transaction details
    private String getStudentDocumentSubmissionDetails(TransactionData transaction) {
        String description = transaction.getDescription();
        StringBuilder details = new StringBuilder();

        try {
            if (description.startsWith("New document submitted: ")) {
                String docName = description.substring("New document submitted: ".length()).trim();
                details.append("Document: ").append(docName).append("\n");
            }

            details.append("Status: Under faculty review\n");
            details.append("Next Step: Await approval/rejection");
        } catch (Exception e) {
            details.append("Document submission completed");
        }

        return details.toString();
    }

    private String getStudentDocumentStatusDetails(TransactionData transaction) {
        String description = transaction.getDescription();
        StringBuilder details = new StringBuilder();

        try {
            if (description.toLowerCase().contains("approved")) {
                details.append("Status: ✅ APPROVED\n");
                details.append("Action: Document accepted\n");
                details.append("Next Step: No further action required");
            } else if (description.toLowerCase().contains("rejected")) {
                details.append("Status: ❌ REJECTED\n");
                details.append("Action: Document not accepted\n");
                details.append("Next Step: Contact faculty for feedback");
            } else {
                details.append("Status: Document status updated\n");
                details.append("Action: Status change recorded");
            }
        } catch (Exception e) {
            details.append("Document status has been updated");
        }

        return details.toString();
    }

    private String getStudentNoticeDetails(TransactionData transaction) {
        String description = transaction.getDescription();
        StringBuilder details = new StringBuilder();

        try {
            details.append("Status: ✅ Successfully posted\n");
            details.append("Visibility: Posted to target audience\n");

            if (description.contains("targeted to:")) {
                details.append("Audience: Specific recipients");
            } else {
                details.append("Audience: All students");
            }
        } catch (Exception e) {
            details.append("Notice posted successfully");
        }

        return details.toString();
    }

    private String getStudentAccountabilityPostedDetails(TransactionData transaction) {
        String description = transaction.getDescription();
        StringBuilder details = new StringBuilder();

        try {
            if (description.contains("Posted accountability: ")) {
                String feeInfo = description.substring("Posted accountability: ".length());
                if (feeInfo.contains(" for ")) {
                    feeInfo = feeInfo.substring(0, feeInfo.indexOf(" for "));
                }
                details.append("Fee: ").append(feeInfo).append("\n");
            }

            details.append("Status: ❌ UNPAID (Default)\n");
            details.append("Action Required: Payment needed");
        } catch (Exception e) {
            details.append("Accountability added to your account");
        }

        return details.toString();
    }

    private String getStudentAccountabilityStatusDetails(TransactionData transaction) {
        String description = transaction.getDescription();
        StringBuilder details = new StringBuilder();

        try {
            if (description.toLowerCase().contains("paid")) {
                details.append("Status: ✅ PAID\n");
                details.append("Payment: Confirmed and recorded\n");
                details.append("Action: No further payment required");
            } else if (description.toLowerCase().contains("unpaid")) {
                details.append("Status: ❌ UNPAID\n");
                details.append("Payment: Still required\n");
                details.append("Action: Please complete payment");
            } else {
                details.append("Status: Payment status updated\n");
                details.append("Action: Status change recorded");
            }
        } catch (Exception e) {
            details.append("Payment status has been updated");
        }

        return details.toString();
    }

    private String getAdditionalTransactionInfo(TransactionData transaction) {
        String actionType = transaction.getActionType();
        String description = transaction.getDescription();

        if ("Faculty Inquiry".equals(actionType)) {
            return "Status: Your inquiry has been submitted and is pending response from faculty.";
        } else if ("Document Submission".equals(actionType)) {
            return "Status: Document has been submitted for faculty review.";
        } else if ("Document Status Update".equals(actionType)) {
            if (description.toLowerCase().contains("approved")) {
                return "Status: ✅ Your document has been approved!";
            } else if (description.toLowerCase().contains("rejected")) {
                return "Status: ❌ Your document was rejected. Please contact faculty for details.";
            }
        } else if ("Notice Posted".equals(actionType)) {
            return "Status: Your notice has been posted and is visible to the target audience.";
        } else if ("Accountability Posted".equals(actionType)) {
            return "Status: ✅ Accountability has been successfully posted to target students.";
        } else if ("Accountability Status Update".equals(actionType)) {
            if (description.toLowerCase().contains("paid")) {
                return "Status: ✅ Accountability status updated to paid.";
            } else if (description.toLowerCase().contains("unpaid")) {
                return "Status: ❌ Accountability status updated to unpaid.";
            }
            return "Status: Accountability payment status has been updated.";
        } else if ("Accountability Deleted".equals(actionType)) {
            return "Status: ⚠️ Accountability has been permanently removed from the system.";
        }

        return "";
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }

    // Transaction data class
    private static class TransactionData {
        private final int id;
        private final int userId;
        private final String actionType;
        private final String description;
        private final long timestamp;

        public TransactionData(int id, int userId, String actionType, String description, long timestamp) {
            this.id = id;
            this.userId = userId;
            this.actionType = actionType != null ? actionType : "";
            this.description = description != null ? description : "";
            this.timestamp = timestamp;
        }

        public int getId() { return id; }
        public int getUserId() { return userId; }
        public String getActionType() { return actionType; }
        public String getDescription() { return description; }
        public long getTimestamp() { return timestamp; }
    }
}