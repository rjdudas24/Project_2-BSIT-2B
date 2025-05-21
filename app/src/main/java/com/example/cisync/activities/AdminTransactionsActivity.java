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
            filterOptions.add("RESEND");

            // Set up spinner adapter
            ArrayAdapter<String> filterAdapter = new ArrayAdapter<>(
                    this, android.R.layout.simple_spinner_item, filterOptions);
            filterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
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

                    // Create display text with special formatting for Document actions
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
            adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, txList);
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

            // Build dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Transaction Details");

            // Create message with details
            String message = "Date: " + formatTimestamp(transaction.getTimestamp()) + "\n\n" +
                    "Action: " + transaction.getActionType() + "\n\n" +
                    "Description: " + transaction.getDescription() + "\n\n" +
                    "User: " + userName + "\n" +
                    "Role: " + userRole + "\n" +
                    "User ID: " + transaction.getUserId();

            // If it's a document-related transaction, try to find more details
            if ("Document Submission".equals(transaction.getActionType()) ||
                    "Document Status Update".equals(transaction.getActionType())) {

                String additionalInfo = getDocumentDetails(transaction);
                if (!additionalInfo.isEmpty()) {
                    message += "\n\nDocument Details:\n" + additionalInfo;
                }
            }

            builder.setMessage(message);
            builder.setPositiveButton("Close", null);
            builder.show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing details: " + e.getMessage(), e);
            Toast.makeText(this, "Error showing transaction details", Toast.LENGTH_SHORT).show();
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