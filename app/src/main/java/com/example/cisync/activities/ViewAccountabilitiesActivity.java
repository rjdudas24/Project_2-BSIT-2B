package com.example.cisync.activities;

import android.app.Activity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.cisync.R;
import com.example.cisync.database.DBHelper;

import java.util.ArrayList;
import java.util.List;

public class ViewAccountabilitiesActivity extends Activity {

    private static final String TAG = "ViewAccountabilities";
    private RecyclerView rvAccountabilities;
    private ImageButton btnBack;
    private DBHelper dbHelper;
    private List<EnhancedAccountability> accountabilityList = new ArrayList<>();
    private EnhancedAccountabilityAdapter adapter;
    private int studentId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_accountabilities);

        // Initialize views
        rvAccountabilities = findViewById(R.id.rvAccountabilities);
        btnBack = findViewById(R.id.btnBack);
        dbHelper = new DBHelper(this);

        // Set up RecyclerView
        rvAccountabilities.setLayoutManager(new LinearLayoutManager(this));

        // Set up back button click listener
        btnBack.setOnClickListener(v -> finish());

        // Get student ID from intent
        studentId = getIntent().getIntExtra("studentId", -1);
        Log.d(TAG, "StudentID: " + studentId);

        if (studentId == -1) {
            Log.e(TAG, "No valid student ID received");
            finish();
            return;
        }

        // Load accountabilities from database
        loadAccountabilities();
    }

    private void loadAccountabilities() {
        accountabilityList.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        try {
            // Enhanced query to get accountability details including who posted them
            String query = "SELECT a.id, a.fee_name, a.amount, a.status, " +
                    "a.posted_by_name, a.posted_by_position, a.created_at " +
                    "FROM accountabilities a " +
                    "WHERE a.student_id = ? " +
                    "ORDER BY a.created_at DESC";

            Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(studentId)});

            Log.d(TAG, "Query result cursor has " + cursor.getCount() + " items");

            if (cursor.moveToFirst()) {
                do {
                    int id = cursor.getInt(0);
                    String feeName = cursor.getString(1);
                    String amount = cursor.getString(2);
                    boolean isPaid = cursor.getInt(3) == 1;
                    String postedByName = cursor.getString(4);
                    String postedByPosition = cursor.getString(5);
                    long createdAt = cursor.getLong(6);

                    // Handle null values for backward compatibility
                    if (postedByName == null || postedByName.isEmpty()) {
                        postedByName = "System";
                    }
                    if (postedByPosition == null || postedByPosition.isEmpty()) {
                        postedByPosition = "Admin";
                    }

                    EnhancedAccountability accountability = new EnhancedAccountability(
                            id, feeName, "₱ " + amount, isPaid, postedByName, postedByPosition, createdAt);
                    accountabilityList.add(accountability);

                } while (cursor.moveToNext());
            } else {
                Log.d(TAG, "No accountabilities found, adding sample data");
                // Add sample data for display only if no real data exists
                addSampleAccountabilities();
            }

            cursor.close();

        } catch (Exception e) {
            Log.e(TAG, "Error loading accountabilities: " + e.getMessage(), e);
            // Add sample data on error
            addSampleAccountabilities();
        } finally {
            db.close();
        }

        // Create and set the adapter
        adapter = new EnhancedAccountabilityAdapter(accountabilityList);
        rvAccountabilities.setAdapter(adapter);

        Log.d(TAG, "Added " + accountabilityList.size() + " items to adapter");
    }

    private void addSampleAccountabilities() {
        accountabilityList.clear();
        accountabilityList.add(new EnhancedAccountability(
                1, "College Fee", "₱ 500", true, "Sample Treasurer", "Treasurer", System.currentTimeMillis()));
        accountabilityList.add(new EnhancedAccountability(
                2, "Fines", "₱ 210", false, "Sample Auditor", "Auditor", System.currentTimeMillis() - 86400000));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }

    // Enhanced accountability class with additional information
    public static class EnhancedAccountability {
        private final int id;
        private final String feeName;
        private final String amount;
        private final boolean isPaid;
        private final String postedByName;
        private final String postedByPosition;
        private final long createdAt;

        public EnhancedAccountability(int id, String feeName, String amount, boolean isPaid,
                                      String postedByName, String postedByPosition, long createdAt) {
            this.id = id;
            this.feeName = feeName;
            this.amount = amount;
            this.isPaid = isPaid;
            this.postedByName = postedByName;
            this.postedByPosition = postedByPosition;
            this.createdAt = createdAt;
        }

        public int getId() { return id; }
        public String getFeeName() { return feeName; }
        public String getAmount() { return amount; }
        public boolean isPaid() { return isPaid; }
        public String getPostedByName() { return postedByName; }
        public String getPostedByPosition() { return postedByPosition; }
        public long getCreatedAt() { return createdAt; }
    }
}