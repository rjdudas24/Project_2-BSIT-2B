package com.example.cisync.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cisync.R;

public class StudentFacultyInquiryActivity extends AppCompatActivity {

    private EditText etFacultyName;
    private ImageButton btnClearFaculty;
    private Spinner spinnerDepartment;
    private TextView tvPurposeTitle;
    private TextView tvPurposeDescription;
    private Button btnSubmit;
    private ImageButton btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faculty_inquiry); // Match this with your XML file name

        // Initialize views
        etFacultyName = findViewById(R.id.etFacultyName);
        btnClearFaculty = findViewById(R.id.btnClearFaculty);
        spinnerDepartment = findViewById(R.id.spinnerDepartment);
        tvPurposeTitle = findViewById(R.id.tvPurposeTitle);
        tvPurposeDescription = findViewById(R.id.tvPurposeDescription);
        btnSubmit = findViewById(R.id.btnSubmit);
        btnBack = findViewById(R.id.btnBack);

        // Populate spinner with single item: "IT"
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new String[]{"IT"}
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDepartment.setAdapter(adapter);

        // Clear faculty name field when clear button is pressed
        btnClearFaculty.setOnClickListener(v -> etFacultyName.setText(""));

        // Submit button functionality
        btnSubmit.setOnClickListener(v -> {
            String facultyName = etFacultyName.getText().toString().trim();
            String department = spinnerDepartment.getSelectedItem().toString();
            String subject = tvPurposeTitle.getText().toString().trim();
            String purpose = tvPurposeDescription.getText().toString().trim();

            // Simple validation
            if (facultyName.isEmpty() || subject.isEmpty() || purpose.isEmpty()) {
                Toast.makeText(this, "Please fill out all fields", Toast.LENGTH_SHORT).show();
            } else {
                // Display a message or send the data to your database/server
                String message = "Faculty: " + facultyName + "\nDept: " + department +
                        "\nSubject: " + subject + "\nPurpose: " + purpose;
                Toast.makeText(this, "Submitted:\n" + message, Toast.LENGTH_LONG).show();
            }
        });

        // Back button functionality
        btnBack.setOnClickListener(v -> finish());
    }
}
