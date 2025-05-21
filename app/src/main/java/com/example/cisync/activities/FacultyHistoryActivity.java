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
    ArrayList<String> historyList = new ArrayList<>();
    ArrayList<Integer> transactionIds = new ArrayList<>();
    ArrayList<Integer> studentIds = new ArrayList<>(); // Store student IDs for each transaction
    ArrayList<String> messages = new ArrayList<>(); // Store original messages
    ArrayAdapter<String> adapter;
    DBHelper dbHelper;
    int facultyId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faculty_history);

        facultyId = getIntent().getIntExtra("userId", -1);
        if (facultyId == -1) {
            // If no valid ID passed, use a static ID for demo purposes
            facultyId = 1; // Default faculty ID
            Log.d(TAG, "Using default faculty ID: " + facultyId);
        } else {
            Log.d(TAG, "Using provided faculty ID: " + facultyId);
        }

        lvHistory = findViewById(R.id.lvHistory);
        btnBackHistory = findViewById(R.id.btnBackHistory);
        dbHelper = new DBHelper(this);
        btnBackHistory.setOnClickListener(v -> finish());
        loadHistory();

        lvHistory.setOnItemClickListener((adapterView, view, position, id) -> {
            if (position < transactionIds.size()) {
                int transactionId = transactionIds.get(position);
                int studentId = studentIds.get(position);
                String originalMessage = messages.get(position);

                // Show confirmation dialog before resending
                showResendConfirmationDialog(transactionId, studentId, originalMessage);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh history when returning to this screen
        loadHistory();
    }

    private void loadHistory() {
        historyList.clear();
        transactionIds.clear();
        studentIds.clear();
        messages.clear();

        SQLiteDatabase db = dbHelper.getReadableDatabase();

        try {
            // Query to get faculty transactions
            Cursor cursor = db.rawQuery(
                    "SELECT id, user_id, description, timestamp, action_type FROM transactions WHERE user_id=? ORDER BY timestamp DESC",
                    new String[]{String.valueOf(facultyId)}
            );

            if (cursor.moveToFirst()) {
                do {
                    int id = cursor.getInt(0);
                    int studentId = cursor.getInt(1);
                    String msg = cursor.getString(2);
                    long time = cursor.getLong(3);
                    String actionType = cursor.getString(4);

                    // Format the time for display
                    String formattedTime = formatTime(time);

                    // Create a formatted display string
                    String displayText = formattedTime + "\n";
                    if (actionType != null && !actionType.isEmpty()) {
                        displayText += actionType + ": ";
                    }
                    displayText += msg;

                    // Add to our lists
                    historyList.add(displayText);
                    transactionIds.add(id);
                    studentIds.add(studentId);
                    messages.add(msg);

                } while (cursor.moveToNext());
            } else {
                // No history found
                historyList.add("No transaction history found");
            }

            cursor.close();

            // Update the adapter
            adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, historyList);
            lvHistory.setAdapter(adapter);

        } catch (Exception e) {
            Log.e(TAG, "Error loading history: " + e.getMessage());
            Toast.makeText(this, "Error loading history", Toast.LENGTH_SHORT).show();
            historyList.add("Error loading transaction history");
            adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, historyList);
            lvHistory.setAdapter(adapter);
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

    private void showResendConfirmationDialog(int transactionId, int studentId, String originalMessage) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Resend Notification");
        builder.setMessage("Do you want to resend this notification?");

        builder.setPositiveButton("Resend", (dialog, which) ->
                resendTransaction(transactionId, studentId, originalMessage)
        );

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void resendTransaction(int originalId, int studentId, String originalMessage) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        try {
            ContentValues cv = new ContentValues();
            cv.put("user_id", studentId);
            cv.put("action_type", "RESEND");
            cv.put("description", "[RESEND] " + originalMessage);
            cv.put("timestamp", System.currentTimeMillis());

            long result = db.insert("transactions", null, cv);

            if (result != -1) {
                Toast.makeText(this, "Notification resent successfully", Toast.LENGTH_SHORT).show();
                loadHistory(); // Refresh the list
            } else {
                Toast.makeText(this, "Failed to resend notification", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error resending: " + e.getMessage());
            Toast.makeText(this, "Error resending notification", Toast.LENGTH_SHORT).show();
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
