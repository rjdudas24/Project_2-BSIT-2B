package com.example.cisync.activities;

import android.app.Activity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cisync.R;
import com.example.cisync.database.DBHelper;

import java.util.ArrayList;
import java.util.List;

public class ViewAccountabilitiesActivity extends Activity {

    private RecyclerView rvAccountabilities;
    private DBHelper dbHelper;
    private List<Accountability> accountabilityList;
    private AccountabilityAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_accountabilities);

        // Initialize UI components
        rvAccountabilities = findViewById(R.id.rvAccountabilities);
        ImageButton btnBack = findViewById(R.id.btnBack);

        // Set up back button click listener
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Set up RecyclerView
        rvAccountabilities.setLayoutManager(new LinearLayoutManager(this));

        // Initialize database helper
        dbHelper = new DBHelper(this);

        // Load data
        accountabilityList = new ArrayList<>();
        loadAccountabilities();

        // Set up adapter
        adapter = new AccountabilityAdapter(accountabilityList);
        rvAccountabilities.setAdapter(adapter);
    }

    private void loadAccountabilities() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // For demo: using static student_id = 1
        Cursor cursor = db.rawQuery(
                "SELECT id, description, '₱' || CAST((id * 500) AS TEXT), " +
                        "(CASE WHEN id % 2 = 0 THEN 1 ELSE 0 END) FROM accountabilities WHERE student_id=?",
                new String[]{"1"});

        if (cursor.moveToFirst()) {
            do {
                String feeName = cursor.getString(1);
                String amount = cursor.getString(2);
                boolean isPaid = cursor.getInt(3) == 1;

                accountabilityList.add(new Accountability(feeName, amount, isPaid));
            } while (cursor.moveToNext());
        }

        cursor.close();


        }
    }
