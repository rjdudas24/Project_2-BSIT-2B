package com.example.cisync.activities;

import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cisync.R;

public class FacultyInquirySent extends AppCompatActivity {

    private Button btnOk;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faculty_inquiry_sent);

        btnOk = findViewById(R.id.btnOk);

        btnOk.setOnClickListener(v -> {
            finish();
        });
    }
}
