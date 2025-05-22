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
            adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, transactionList);
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
            // Build dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Transaction Details");

            // Create message with details
            String message = "Date: " + formatTimestamp(transaction.getTimestamp()) + "\n\n" +
                    "Action: " + transaction.getActionType() + "\n\n" +
                    "Description: " + transaction.getDescription() + "\n\n" +
                    "Transaction ID: " + transaction.getId();

            // Add specific details based on transaction type
            String additionalInfo = getAdditionalTransactionInfo(transaction);
            if (!additionalInfo.isEmpty()) {
                message += "\n\n" + additionalInfo;
            }

            builder.setMessage(message);
            builder.setPositiveButton("Close", null);
            builder.show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing details: " + e.getMessage(), e);
            Toast.makeText(this, "Error showing transaction details", Toast.LENGTH_SHORT).show();
        }
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