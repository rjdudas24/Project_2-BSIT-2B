package com.example.cisync.activities;

import android.app.Activity;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import com.example.cisync.R;
import com.example.cisync.database.DBHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RegisterActivity extends Activity {
    EditText etName, etEmail, etPassword;
    RadioGroup rgRole;
    CheckBox cbOrgMember;
    Spinner spOrgPosition;
    Button btnRegister, btnBack;
    DBHelper dbHelper;

    // Define the organization positions
    private static final List<String> ORG_POSITIONS = Arrays.asList(
            "Chairperson",
            "Vice-Chairperson (Internal)",
            "Vice-Chairperson (External)",
            "Secretary",
            "Associate Secretary",
            "Treasurer",
            "Associate Treasurer",
            "Auditor"
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize UI components
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        rgRole = findViewById(R.id.rgRole);
        cbOrgMember = findViewById(R.id.cbOrgMember);
        spOrgPosition = findViewById(R.id.spOrgPosition); // New spinner replacing etOrgRole
        btnRegister = findViewById(R.id.btnRegister);
        btnBack = findViewById(R.id.btnBack);

        dbHelper = new DBHelper(this);

        // Setup organization position spinner
        setupOrgPositionSpinner();

        // Setup role radio group listener
        rgRole.setOnCheckedChangeListener((group, checkedId) -> {
            RadioButton selected = findViewById(checkedId);
            boolean isStudent = selected.getText().toString().equals("Student");
            cbOrgMember.setVisibility(isStudent ? View.VISIBLE : View.GONE);

            // Hide or show organization position spinner based on selection
            updateOrgPositionVisibility();
        });

        // Setup organization membership checkbox listener
        cbOrgMember.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateOrgPositionVisibility();
        });

        btnRegister.setOnClickListener(v -> registerUser());
        btnBack.setOnClickListener(v -> finish());
    }

    /**
     * Setup the organization position spinner with predefined positions
     */
    private void setupOrgPositionSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                ORG_POSITIONS
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spOrgPosition.setAdapter(adapter);
    }

    /**
     * Update the visibility of the organization position spinner
     * based on the role and org membership selections
     */
    private void updateOrgPositionVisibility() {
        if (rgRole.getCheckedRadioButtonId() != -1) {
            RadioButton selected = findViewById(rgRole.getCheckedRadioButtonId());
            boolean isStudent = selected.getText().toString().equals("Student");
            boolean isOrgMember = cbOrgMember.isChecked();

            spOrgPosition.setVisibility((isStudent && isOrgMember) ? View.VISIBLE : View.GONE);

            // If position spinner is now visible, also show a label for it
            View positionLabel = findViewById(R.id.tvOrgPositionLabel);
            if (positionLabel != null) {
                positionLabel.setVisibility((isStudent && isOrgMember) ? View.VISIBLE : View.GONE);
            }
        }
    }

    /**
     * Register the user with the provided information
     */
    private void registerUser() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        int selectedId = rgRole.getCheckedRadioButtonId();

        // Validate inputs
        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || selectedId == -1) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get selected role
        RadioButton selected = findViewById(selectedId);
        String role = selected.getText().toString();

        // Check if user is a student with organization membership
        int hasOrg = (role.equals("Student") && cbOrgMember.isChecked()) ? 1 : 0;
        String orgPosition = "";

        if (hasOrg == 1) {
            orgPosition = spOrgPosition.getSelectedItem().toString();
        }

        // Save user to database
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("email", email);
        values.put("password", password);
        values.put("role", role);
        values.put("has_org", hasOrg);
        values.put("org_role", orgPosition);

        long result = db.insert("users", null, values);
        if (result != -1) {
            Toast.makeText(this, "Registered successfully!", Toast.LENGTH_SHORT).show();
            finish(); // Go back to Login
        } else {
            Toast.makeText(this, "Registration failed. Email may already exist.", Toast.LENGTH_SHORT).show();
        }
    }
}