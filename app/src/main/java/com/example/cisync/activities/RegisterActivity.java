package com.example.cisync.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import com.example.cisync.R;
import com.example.cisync.database.DBHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RegisterActivity extends Activity {
    EditText etFirstName, etMiddleName, etLastName, etEmail, etPassword, etRepeatPassword;
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
        etFirstName = findViewById(R.id.etFirstName);
        etMiddleName = findViewById(R.id.etMiddleName);
        etLastName = findViewById(R.id.etLastName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etRepeatPassword = findViewById(R.id.etRepeatPassword);
        rgRole = findViewById(R.id.rgRole);
        cbOrgMember = findViewById(R.id.cbOrgMember);
        spOrgPosition = findViewById(R.id.spOrgPosition); // New spinner replacing etOrgRole
        btnRegister = findViewById(R.id.btnRegister);
        btnBack = findViewById(R.id.btnBack);

        dbHelper = new DBHelper(this);

        // Setup organization position spinner
        setupOrgPositionSpinner();

        if (rgRole != null) {
            rgRole.setOnCheckedChangeListener((group, checkedId) -> updateOrgPositionVisibility());
        }

        if (cbOrgMember != null) {
            cbOrgMember.setOnCheckedChangeListener((buttonView, isChecked) -> updateOrgPositionVisibility());
        }

        btnRegister.setOnClickListener(v -> registerUser());
        btnBack.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, WelcomeActivity.class));
            finish();
        });
    }

    /**
     * Setup the organization position spinner with predefined positions
     */
    private void setupOrgPositionSpinner() {
        if (spOrgPosition != null) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_spinner_item,
                    ORG_POSITIONS
            );
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spOrgPosition.setAdapter(adapter);
        }
    }

    /**
     * Update the visibility of the organization position spinner
     * based on the role and org membership selections
     */
    private void updateOrgPositionVisibility() {
        if (rgRole != null && cbOrgMember != null && spOrgPosition != null) {
            RadioButton selected = findViewById(rgRole.getCheckedRadioButtonId());
            boolean isStudent = selected != null && selected.getText().toString().equals("Student");
            boolean isOrgMember = cbOrgMember.isChecked();

            int visibility = (isStudent && isOrgMember) ? View.VISIBLE : View.GONE;
            spOrgPosition.setVisibility(visibility);
        }
    }


    /**
     * Register the user with the provided information
     */
    private void registerUser() {
        String firstName = etFirstName.getText().toString().trim();
        String middleName = etMiddleName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();
        String fullName = firstName + " " + middleName + " " + lastName;
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String repeatPassword = etRepeatPassword.getText().toString().trim();
        int selectedId = rgRole.getCheckedRadioButtonId();

        // Validate inputs
        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || password.isEmpty() || repeatPassword.isEmpty() || selectedId == -1) {
            Toast.makeText(this, "Please complete all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(repeatPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get selected role
        RadioButton selectedRole = findViewById(selectedId);
        String role = selectedRole.getText().toString();

        // Check if user is a student with organization membership
        int hasOrg = (cbOrgMember != null && cbOrgMember.isChecked() && role.equals("Student")) ? 1 : 0;
        String orgPosition = (hasOrg == 1 && spOrgPosition != null) ? spOrgPosition.getSelectedItem().toString() : "";

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // Check for existing email
        Cursor cursor = db.query("users", null, "email = ?", new String[]{email}, null, null, null);
        if (cursor.moveToFirst()) {
            Toast.makeText(this, "Email already exists.", Toast.LENGTH_SHORT).show();
            cursor.close();
            db.close();
            return;
        }
        cursor.close();

        // Save user to database
        ContentValues values = new ContentValues();
        values.put("name", fullName);
        values.put("email", email);
        values.put("password", password);
        values.put("role", role);
        values.put("has_org", hasOrg);
        values.put("org_role", orgPosition);

        long result = db.insert("users", null, values);
        if (result != -1) {
            Toast.makeText(this, "Registered successfully!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(RegisterActivity.this, com.example.cisync.activities.WelcomeActivity.class));
            finish();
        } else {
            Toast.makeText(this, "Registration failed.", Toast.LENGTH_SHORT).show();
        }

        db.close();
    }
}