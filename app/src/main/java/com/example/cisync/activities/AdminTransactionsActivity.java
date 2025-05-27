package com.example.cisync.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.view.View;
import android.os.Bundle;
import android.widget.*;
import com.example.cisync.R;
import com.example.cisync.database.DBHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class AdminTransactionsActivity extends Activity {

    private static final String TAG = "AdminTransactions";
    ListView lvAllTransactions;
    TextView tvNoTransactions;
    Spinner spinnerTransactionFilter;
    Button btnApplyFilter, btnClearFilter;
    ImageView btnBackTransactions;

    DBHelper dbHelper;
    ArrayList<String> txList = new ArrayList<>();
    ArrayList<TransactionData> transactionsList = new ArrayList<>();
    ArrayAdapter<String> adapter;
    String currentFilter = "All"; // Default filter

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_transactions);

        // Initialize views
        initializeViews();

        // Set up filter spinner
        setupFilterSpinner();

        // Set up click listeners
        setupClickListeners();

        // Load initial data
        loadTransactions(null);
    }

    private void initializeViews() {
        try {
            lvAllTransactions = findViewById(R.id.lvAllTransactions);
            tvNoTransactions = findViewById(R.id.tvNoTransactions);
            spinnerTransactionFilter = findViewById(R.id.spinnerTransactionFilter);
            btnApplyFilter = findViewById(R.id.btnApplyFilter);
            btnClearFilter = findViewById(R.id.btnClearFilter);
            btnBackTransactions = findViewById(R.id.btnBackTransactions);
            dbHelper = new DBHelper(this);
        } catch (Exception e) {
            Log.e(TAG, "Error initializing views: " + e.getMessage(), e);
            Toast.makeText(this, "Error initializing views", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupFilterSpinner() {
        try {
            // Create filter options - Add Document Submission and Document Status Update
            ArrayList<String> filterOptions = new ArrayList<>();
            filterOptions.add("All");
            filterOptions.add("Registration");
            filterOptions.add("User Verification");
            filterOptions.add("Account Status Change");
            filterOptions.add("User Update");
            filterOptions.add("Document Submission"); // Added for Track Documents
            filterOptions.add("Document Status Update"); // Added for Track Documents
            filterOptions.add("Accountability Posted"); // Added for Accountability Management
            filterOptions.add("Accountability Status Update"); // Added for Accountability Management
            filterOptions.add("Accountability Deleted"); // Added for Accountability Management
            filterOptions.add("Faculty Inquiry"); // Added for Faculty Inquiries
            filterOptions.add("Notice Posted"); // Added for Notices
            filterOptions.add("RESEND");

            // Set up spinner adapter
            ArrayAdapter<String> filterAdapter = new ArrayAdapter<>(
                    this, R.layout.custom_role_spinner, filterOptions);
            filterAdapter.setDropDownViewResource(R.layout.custom_role_spinner);
            spinnerTransactionFilter.setAdapter(filterAdapter);
        } catch (Exception e) {
            Log.e(TAG, "Error setting up filter spinner: " + e.getMessage(), e);
        }
    }

    private void setupClickListeners() {
        try {
            // Back button
            btnBackTransactions.setOnClickListener(v -> finish());

            // Apply filter button
            btnApplyFilter.setOnClickListener(v -> {
                currentFilter = spinnerTransactionFilter.getSelectedItem().toString();
                if (currentFilter.equals("All")) {
                    loadTransactions(null);
                } else {
                    loadTransactions(currentFilter);
                }
            });

            // Clear filter button
            btnClearFilter.setOnClickListener(v -> {
                spinnerTransactionFilter.setSelection(0); // Set to "All"
                currentFilter = "All";
                loadTransactions(null);
            });

            // List item click listener for viewing details
            lvAllTransactions.setOnItemClickListener((parent, view, position, id) -> {
                if (position < transactionsList.size()) {
                    TransactionData transaction = transactionsList.get(position);
                    showTransactionDetailsDialog(transaction);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error setting up click listeners: " + e.getMessage(), e);
        }
    }

    private void loadTransactions(String filter) {
        txList.clear();
        transactionsList.clear();

        SQLiteDatabase db = dbHelper.getReadableDatabase();

        try {
            String query;
            String[] selectionArgs;

            if (filter == null || filter.equals("All")) {
                // No filter - get all transactions
                query = "SELECT id, user_id, action_type, description, timestamp FROM transactions ORDER BY timestamp DESC";
                selectionArgs = null;
            } else {
                // Apply filter by action_type
                query = "SELECT id, user_id, action_type, description, timestamp FROM transactions " +
                        "WHERE action_type LIKE ? ORDER BY timestamp DESC";
                selectionArgs = new String[]{"%" + filter + "%"};
            }

            Cursor cursor = db.rawQuery(query, selectionArgs);

            if (cursor.moveToFirst() && cursor.getCount() > 0) {
                // Hide empty message, show list
                tvNoTransactions.setVisibility(View.GONE);
                lvAllTransactions.setVisibility(View.VISIBLE);

                do {
                    int id = cursor.getInt(0);
                    int userId = cursor.getInt(1);
                    String actionType = cursor.getString(2);
                    String description = cursor.getString(3);
                    long timestamp = cursor.getLong(4);

                    // Create transaction object
                    TransactionData transaction = new TransactionData(
                            id, userId, actionType, description, timestamp);
                    transactionsList.add(transaction);

                    // Format for display
                    String formattedDate = formatTimestamp(timestamp);
                    String userInfo = getUserInfo(userId);

                    // Create display text with special formatting for different action types
                    String displayText;

                    if ("Document Submission".equals(actionType)) {
                        displayText = "📄 " + formattedDate + "\n" +
                                actionType;

                        if (!userInfo.isEmpty()) {
                            displayText += " (" + userInfo + ")";
                        }

                        displayText += ":\n" + description;
                    }
                    else if ("Document Status Update".equals(actionType)) {
                        displayText = "📝 " + formattedDate + "\n" +
                                actionType;

                        if (!userInfo.isEmpty()) {
                            displayText += " (" + userInfo + ")";
                        }

                        displayText += ":\n" + description;
                    }
                    else if ("Accountability Posted".equals(actionType)) {
                        displayText = "💰 " + formattedDate + "\n" +
                                actionType;

                        if (!userInfo.isEmpty()) {
                            displayText += " (" + userInfo + ")";
                        }

                        displayText += ":\n" + description;
                    }
                    else if ("Accountability Status Update".equals(actionType)) {
                        displayText = "💳 " + formattedDate + "\n" +
                                actionType;

                        if (!userInfo.isEmpty()) {
                            displayText += " (" + userInfo + ")";
                        }

                        displayText += ":\n" + description;
                    }
                    else if ("Accountability Deleted".equals(actionType)) {
                        displayText = "🗑️ " + formattedDate + "\n" +
                                actionType;

                        if (!userInfo.isEmpty()) {
                            displayText += " (" + userInfo + ")";
                        }

                        displayText += ":\n" + description;
                    }
                    else if ("Faculty Inquiry".equals(actionType)) {
                        displayText = "❓ " + formattedDate + "\n" +
                                actionType;

                        if (!userInfo.isEmpty()) {
                            displayText += " (" + userInfo + ")";
                        }

                        displayText += ":\n" + description;
                    }
                    else if ("Notice Posted".equals(actionType)) {
                        displayText = "📢 " + formattedDate + "\n" +
                                actionType;

                        if (!userInfo.isEmpty()) {
                            displayText += " (" + userInfo + ")";
                        }

                        displayText += ":\n" + description;
                    }
                    else {
                        displayText = formattedDate + "\n" +
                                (actionType != null ? actionType : "Unknown action");

                        if (!userInfo.isEmpty()) {
                            displayText += " (" + userInfo + ")";
                        }

                        displayText += ":\n" + description;
                    }

                    txList.add(displayText);
                } while (cursor.moveToNext());

            } else {
                // No transactions found - show message
                tvNoTransactions.setVisibility(View.VISIBLE);
                lvAllTransactions.setVisibility(View.GONE);

                tvNoTransactions.setText("No transactions found" +
                        (filter != null && !filter.equals("All") ? " for filter: " + filter : ""));
            }

            cursor.close();

            // Update adapter
            adapter = new ArrayAdapter<>(this, R.layout.custom_login_history_item, txList);
            lvAllTransactions.setAdapter(adapter);

        } catch (Exception e) {
            Log.e(TAG, "Error loading transactions: " + e.getMessage(), e);
            Toast.makeText(this, "Error loading transactions: " + e.getMessage(), Toast.LENGTH_SHORT).show();

            // Show error state
            tvNoTransactions.setVisibility(View.VISIBLE);
            tvNoTransactions.setText("Error loading transactions");
            lvAllTransactions.setVisibility(View.GONE);
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

    private String getUserInfo(int userId) {
        String userInfo = "";
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        try {
            Cursor cursor = db.rawQuery(
                    "SELECT name, role FROM users WHERE id = ?",
                    new String[]{String.valueOf(userId)}
            );

            if (cursor.moveToFirst()) {
                String name = cursor.getString(0);
                String role = cursor.getString(1);
                userInfo = name + ", " + role;
            }

            cursor.close();
        } catch (Exception e) {
            Log.e(TAG, "Error getting user info: " + e.getMessage(), e);
        }

        return userInfo;
    }

    private void showTransactionDetailsDialog(TransactionData transaction) {
        try {
            // Create custom dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            // Inflate custom layout
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_transaction_details, null);
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
            TextView tvUserName = dialogView.findViewById(R.id.tvDialogUserName);
            TextView tvUserRole = dialogView.findViewById(R.id.tvDialogUserRole);
            TextView tvUserId = dialogView.findViewById(R.id.tvDialogUserId);
            TextView tvDescription = dialogView.findViewById(R.id.tvDialogDescription);
            TextView tvTransactionIcon = dialogView.findViewById(R.id.tvTransactionIcon);
            TextView tvTransactionCategory = dialogView.findViewById(R.id.tvTransactionCategory);
            TextView tvTransactionSubcategory = dialogView.findViewById(R.id.tvTransactionSubcategory);
            View vStatusIndicator = dialogView.findViewById(R.id.vTransactionStatusIndicator);
            LinearLayout llAdditionalDetails = dialogView.findViewById(R.id.llAdditionalDetails);
            LinearLayout llAdditionalDetailsContent = dialogView.findViewById(R.id.llAdditionalDetailsContent);
            TextView tvAdditionalDetailsTitle = dialogView.findViewById(R.id.tvAdditionalDetailsTitle);
            Button btnDialogClose = dialogView.findViewById(R.id.btnDialogClose);

            // Get user details
            String userInfo = getUserInfo(transaction.getUserId());
            String userName = "Unknown";
            String userRole = "Unknown";

            if (!userInfo.isEmpty()) {
                String[] parts = userInfo.split(", ");
                if (parts.length >= 2) {
                    userName = parts[0];
                    userRole = parts[1];
                }
            }

            // Set basic transaction information
            tvTransactionId.setText(String.valueOf(transaction.getId()));
            tvActionType.setText(transaction.getActionType());
            tvTransactionTime.setText(formatTimestamp(transaction.getTimestamp()));
            tvDescription.setText(transaction.getDescription());

            // Set user information
            tvUserName.setText(userName);
            tvUserRole.setText(userRole);
            tvUserId.setText(String.valueOf(transaction.getUserId()));

            // Set role-specific styling for user role
            switch (userRole.toLowerCase()) {
                case "admin":
                    tvUserRole.setBackgroundColor(getResources().getColor(R.color.admin_role_color));
                    break;
                case "faculty":
                    tvUserRole.setBackgroundColor(getResources().getColor(R.color.faculty_role_color));
                    break;
                case "student":
                    tvUserRole.setBackgroundColor(getResources().getColor(R.color.student_role_color));
                    break;
                default:
                    tvUserRole.setBackgroundColor(getResources().getColor(R.color.default_role_color));
                    break;
            }

            // Set transaction-specific details
            String actionType = transaction.getActionType();
            setTransactionTypeDetails(actionType, tvTransactionIcon, tvTransactionCategory,
                    tvTransactionSubcategory, tvActionType, vStatusIndicator);

            // Add additional details if available
            String additionalInfo = getAdditionalTransactionDetails(transaction);
            if (!additionalInfo.isEmpty()) {
                llAdditionalDetails.setVisibility(View.VISIBLE);
                populateAdditionalDetails(llAdditionalDetailsContent, additionalInfo, actionType, tvAdditionalDetailsTitle);
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
            showFallbackTransactionDialog(transaction);
        }
    }

    private void setTransactionTypeDetails(String actionType, TextView tvIcon, TextView tvCategory,
                                           TextView tvSubcategory, TextView tvActionType, View vStatusIndicator) {
        try {
            switch (actionType) {
                case "Document Submission":
                    tvIcon.setText("📄");
                    tvCategory.setText("Document Management");
                    tvSubcategory.setText("Student document submission");
                    tvActionType.setBackgroundColor(getResources().getColor(R.color.document_action_color));
                    vStatusIndicator.setBackgroundResource(R.drawable.status_indicator_active);
                    break;

                case "Document Status Update":
                    tvIcon.setText("📝");
                    tvCategory.setText("Document Management");
                    tvSubcategory.setText("Document status change");
                    tvActionType.setBackgroundColor(getResources().getColor(R.color.document_action_color));
                    vStatusIndicator.setBackgroundResource(R.drawable.status_indicator_active);
                    break;

                case "Accountability Posted":
                    tvIcon.setText("💰");
                    tvCategory.setText("Financial Management");
                    tvSubcategory.setText("New accountability added");
                    tvActionType.setBackgroundColor(getResources().getColor(R.color.accountability_action_color));
                    vStatusIndicator.setBackgroundResource(R.drawable.status_indicator_active);
                    break;

                case "Accountability Status Update":
                    tvIcon.setText("💳");
                    tvCategory.setText("Financial Management");
                    tvSubcategory.setText("Payment status changed");
                    tvActionType.setBackgroundColor(getResources().getColor(R.color.accountability_action_color));
                    vStatusIndicator.setBackgroundResource(R.drawable.status_indicator_active);
                    break;

                case "Accountability Deleted":
                    tvIcon.setText("🗑️");
                    tvCategory.setText("Financial Management");
                    tvSubcategory.setText("Accountability removed");
                    tvActionType.setBackgroundColor(getResources().getColor(R.color.delete_action_color));
                    vStatusIndicator.setBackgroundResource(R.drawable.status_indicator_ended);
                    break;

                case "Faculty Inquiry":
                    tvIcon.setText("❓");
                    tvCategory.setText("Communication");
                    tvSubcategory.setText("Faculty inquiry submitted");
                    tvActionType.setBackgroundColor(getResources().getColor(R.color.inquiry_action_color));
                    vStatusIndicator.setBackgroundResource(R.drawable.status_indicator_active);
                    break;

                case "Notice Posted":
                    tvIcon.setText("📢");
                    tvCategory.setText("Communication");
                    tvSubcategory.setText("Notice published");
                    tvActionType.setBackgroundColor(getResources().getColor(R.color.notice_action_color));
                    vStatusIndicator.setBackgroundResource(R.drawable.status_indicator_active);
                    break;

                case "Registration":
                    tvIcon.setText("👤");
                    tvCategory.setText("User Management");
                    tvSubcategory.setText("New user registration");
                    tvActionType.setBackgroundColor(getResources().getColor(R.color.user_action_color));
                    vStatusIndicator.setBackgroundResource(R.drawable.status_indicator_active);
                    break;

                case "User Verification":
                    tvIcon.setText("✅");
                    tvCategory.setText("User Management");
                    tvSubcategory.setText("User account verified");
                    tvActionType.setBackgroundColor(getResources().getColor(R.color.user_action_color));
                    vStatusIndicator.setBackgroundResource(R.drawable.status_indicator_active);
                    break;

                case "Account Status Change":
                    tvIcon.setText("🔄");
                    tvCategory.setText("User Management");
                    tvSubcategory.setText("Account status modified");
                    tvActionType.setBackgroundColor(getResources().getColor(R.color.user_action_color));
                    vStatusIndicator.setBackgroundResource(R.drawable.status_indicator_active);
                    break;

                case "User Update":
                    tvIcon.setText("📝");
                    tvCategory.setText("User Management");
                    tvSubcategory.setText("User information updated");
                    tvActionType.setBackgroundColor(getResources().getColor(R.color.user_action_color));
                    vStatusIndicator.setBackgroundResource(R.drawable.status_indicator_active);
                    break;

                default:
                    tvIcon.setText("⚙️");
                    tvCategory.setText("System");
                    tvSubcategory.setText("General system action");
                    tvActionType.setBackgroundColor(getResources().getColor(R.color.default_role_color));
                    vStatusIndicator.setBackgroundResource(R.drawable.status_indicator_active);
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting transaction type details: " + e.getMessage(), e);
        }
    }

    private String getAdditionalTransactionDetails(TransactionData transaction) {
        String additionalInfo = "";

        try {
            String actionType = transaction.getActionType();

            if ("Document Submission".equals(actionType) || "Document Status Update".equals(actionType)) {
                additionalInfo = getDocumentDetails(transaction);
            } else if ("Accountability Posted".equals(actionType)) {
                additionalInfo = getAccountabilityPostedDetails(transaction);
            } else if ("Accountability Status Update".equals(actionType)) {
                additionalInfo = getAccountabilityStatusDetails(transaction);
            } else if ("Accountability Deleted".equals(actionType)) {
                additionalInfo = getAccountabilityDeletedDetails(transaction);
            } else if ("Faculty Inquiry".equals(actionType)) {
                additionalInfo = "Inquiry Type: Faculty Communication\nStatus: Submitted for review";
            } else if ("Notice Posted".equals(actionType)) {
                additionalInfo = getNoticePostedDetails(transaction);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting additional transaction details: " + e.getMessage(), e);
        }

        return additionalInfo;
    }

    private void populateAdditionalDetails(LinearLayout container, String additionalInfo,
                                           String actionType, TextView titleView) {
        try {
            container.removeAllViews();

            // Set appropriate title based on action type
            switch (actionType) {
                case "Document Submission":
                case "Document Status Update":
                    titleView.setText("DOCUMENT DETAILS");
                    break;
                case "Accountability Posted":
                case "Accountability Status Update":
                case "Accountability Deleted":
                    titleView.setText("FINANCIAL DETAILS");
                    break;
                case "Faculty Inquiry":
                    titleView.setText("INQUIRY DETAILS");
                    break;
                case "Notice Posted":
                    titleView.setText("NOTICE DETAILS");
                    break;
                default:
                    titleView.setText("ADDITIONAL DETAILS");
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
            Log.e(TAG, "Error populating additional details: " + e.getMessage(), e);
        }
    }

    private void showFallbackTransactionDialog(TransactionData transaction) {
        try {
            // Get user details
            String userInfo = getUserInfo(transaction.getUserId());
            String userName = "Unknown";
            String userRole = "Unknown";

            if (!userInfo.isEmpty()) {
                String[] parts = userInfo.split(", ");
                if (parts.length >= 2) {
                    userName = parts[0];
                    userRole = parts[1];
                }
            }

            AlertDialog.Builder fallbackBuilder = new AlertDialog.Builder(this);
            fallbackBuilder.setTitle("Transaction Details");

            String message = "Date: " + formatTimestamp(transaction.getTimestamp()) + "\n\n" +
                    "Action: " + transaction.getActionType() + "\n\n" +
                    "Description: " + transaction.getDescription() + "\n\n" +
                    "User: " + userName + "\n" +
                    "Role: " + userRole + "\n" +
                    "User ID: " + transaction.getUserId();

            // Add additional details
            String additionalInfo = getAdditionalTransactionDetails(transaction);
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


    // Helper method to get document details for document-related transactions
    private String getDocumentDetails(TransactionData transaction) {
        String details = "";
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        try {
            // Extract document name from description for Document Submission
            String docName = "";

            if ("Document Submission".equals(transaction.getActionType())) {
                String desc = transaction.getDescription();
                if (desc.startsWith("New document submitted: ")) {
                    docName = desc.substring("New document submitted: ".length()).trim();
                }
            }
            else if ("Document Status Update".equals(transaction.getActionType())) {
                String desc = transaction.getDescription();
                if (desc.startsWith("Your document '") && desc.indexOf("'") != desc.lastIndexOf("'")) {
                    docName = desc.substring(desc.indexOf("'") + 1, desc.lastIndexOf("'")).trim();
                }
            }

            if (!docName.isEmpty()) {
                // Query document details
                Cursor cursor = db.rawQuery(
                        "SELECT status, created_by, timestamp FROM documents WHERE name = ?",
                        new String[]{docName}
                );

                if (cursor.moveToFirst()) {
                    String status = cursor.getString(0);
                    String createdBy = cursor.getString(1);
                    String timestamp = cursor.getString(2);

                    details += "Document Name: " + docName + "\n";
                    details += "Status: " + status + "\n";
                    details += "Created by: " + createdBy + "\n";
                    details += "Created on: " + timestamp;
                }

                cursor.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting document details: " + e.getMessage(), e);
        }

        return details;
    }

    // Helper method to get accountability posted details
    private String getAccountabilityPostedDetails(TransactionData transaction) {
        String description = transaction.getDescription();
        StringBuilder details = new StringBuilder();

        try {
            if (description.contains("Posted accountability: ")) {
                String feeInfo = description.substring("Posted accountability: ".length());
                details.append("Fee: ").append(feeInfo).append("\n");

                if (description.contains("for all students")) {
                    details.append("Target: All Students");
                    if (description.contains("(") && description.contains(" students)")) {
                        String countPart = description.substring(description.lastIndexOf("(") + 1, description.lastIndexOf(" students)"));
                        details.append(" (").append(countPart).append(" affected)");
                    }
                } else if (description.contains(" for ")) {
                    String target = description.substring(description.lastIndexOf(" for ") + 5);
                    details.append("Target: ").append(target);
                }
            }
        } catch (Exception e) {
            details.append("Details unavailable");
        }

        return details.toString();
    }

    // Helper method to get accountability status details
    private String getAccountabilityStatusDetails(TransactionData transaction) {
        String description = transaction.getDescription();
        StringBuilder details = new StringBuilder();

        try {
            if (description.toLowerCase().contains("paid")) {
                details.append("Status: Marked as PAID ✅\n");
            } else if (description.toLowerCase().contains("unpaid")) {
                details.append("Status: Marked as UNPAID ❌\n");
            }

            if (description.contains(" for ")) {
                String studentPart = description.substring(description.lastIndexOf(" for ") + 5);
                details.append("Student: ").append(studentPart);
            }
        } catch (Exception e) {
            details.append("Details unavailable");
        }

        return details.toString();
    }

    // Helper method to get accountability deleted details
    private String getAccountabilityDeletedDetails(TransactionData transaction) {
        String description = transaction.getDescription();
        StringBuilder details = new StringBuilder();

        try {
            details.append("Action: Permanent removal\n");
            if (description.contains("Deleted accountability: ")) {
                String feeInfo = description.substring("Deleted accountability: ".length());
                details.append("Removed: ").append(feeInfo);
            }
        } catch (Exception e) {
            details.append("Details unavailable");
        }

        return details.toString();
    }

    // Helper method to get notice posted details
    private String getNoticePostedDetails(TransactionData transaction) {
        String description = transaction.getDescription();
        StringBuilder details = new StringBuilder();

        try {
            if (description.contains("Posted notice: ")) {
                String noticeInfo = description.substring("Posted notice: ".length());
                details.append("Notice: ").append(noticeInfo).append("\n");
            }

            if (description.contains("targeted to:")) {
                details.append("Target: Specific Student");
            } else {
                details.append("Target: All Students");
            }
        } catch (Exception e) {
            details.append("Details unavailable");
        }

        return details.toString();
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