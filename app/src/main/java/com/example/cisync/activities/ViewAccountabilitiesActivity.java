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

    private RecyclerView rvAccountabilities;
    private ImageButton btnBack;
    private DBHelper dbHelper;
    private List<Accountability> accountabilityList = new ArrayList<>();
    private AccountabilityAdapter adapter;
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
        Log.d("ViewAccountabilities", "StudentID: " + studentId);

        // Test code: Add sample data for testing (remove in production)
        if (studentId != -1) {
            dbHelper.addSampleAccountabilities(studentId);
        }

        // Load accountabilities from database
        loadAccountabilities();
    }

    private void loadAccountabilities() {
        accountabilityList.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Use the actual student ID passed from intent
        if (studentId != -1) {
            // Fixed query to match your database schema
            Cursor cursor = db.rawQuery(
                    "SELECT fee_name, amount, status FROM accountabilities WHERE student_id=?",
                    new String[]{String.valueOf(studentId)}
            );

            Log.d("ViewAccountabilities", "Query result cursor has " + cursor.getCount() + " items");

            if (cursor.moveToFirst()) {
                do {
                    String feeName = cursor.getString(0);  // fee_name column
                    String amount = "P " + cursor.getString(1);  // amount column with peso sign
                    boolean isPaid = cursor.getInt(2) == 1;  // status column as boolean

                    accountabilityList.add(new Accountability(feeName, amount, isPaid));
                } while (cursor.moveToNext());
            }

            cursor.close();

            // If no data found, add sample data for display only (not saved to DB)
            if (accountabilityList.isEmpty()) {
                Log.d("ViewAccountabilities", "No data found, adding sample display data");
                accountabilityList.add(new Accountability("College Fee", "P 500", true));
                accountabilityList.add(new Accountability("Fines", "P 210", false));
            }
        }

        // Create and set the adapter
        adapter = new AccountabilityAdapter(accountabilityList);
        rvAccountabilities.setAdapter(adapter);

        Log.d("ViewAccountabilities", "Added " + accountabilityList.size() + " items to adapter");
    }
}