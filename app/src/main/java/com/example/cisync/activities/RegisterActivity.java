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

public class RegisterActivity extends Activity {
    EditText etIdNumber, etFirstName, etMiddleName, etLastName, etEmail, etPassword, etRepeatPassword;
    RadioGroup rgRole;
    RadioButton rbStudent, rbTeacher;
    CheckBox cbOrgMember;
    Spinner spOrgPosition;
    LinearLayout layoutOrgPosition;
    Button btnRegister, btnBack;
    DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize UI elements
        etIdNumber       = findViewById(R.id.etIdNumber);
        etFirstName      = findViewById(R.id.etFirstName);
        etMiddleName     = findViewById(R.id.etMiddleName);
        etLastName       = findViewById(R.id.etLastName);
        etEmail          = findViewById(R.id.etEmail);
        etPassword       = findViewById(R.id.etPassword);
        etRepeatPassword = findViewById(R.id.etRepeatPassword);

        rgRole           = findViewById(R.id.rgRole);
        rbStudent        = findViewById(R.id.rbStudent);
        rbTeacher        = findViewById(R.id.rbTeacher);

        cbOrgMember      = findViewById(R.id.cbOrgMember);
        spOrgPosition    = findViewById(R.id.spOrgPosition);
        layoutOrgPosition= findViewById(R.id.layoutOrgPosition);

        btnRegister      = findViewById(R.id.btnRegister);
        btnBack          = findViewById(R.id.btnBack);

        dbHelper = new DBHelper(this);

        setupOrgPositionSpinner();

        findViewById(R.id.imageView6).setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, WelcomeActivity.class);
            startActivity(intent);
            finish(); // Close this activity to prevent it from staying in the background
        });

        // Role toggle logic
        rgRole.setOnCheckedChangeListener((group, checkedId) -> {
            boolean studentSelected = (checkedId == R.id.rbStudent);
            cbOrgMember.setVisibility(studentSelected ? View.VISIBLE : View.GONE);
            layoutOrgPosition.setVisibility(studentSelected && cbOrgMember.isChecked() ? View.VISIBLE : View.GONE);
        });

        cbOrgMember.setOnCheckedChangeListener((button, isChecked) -> {
            if (rbStudent.isChecked()) {
                layoutOrgPosition.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            }
        });

        btnRegister.setOnClickListener(v -> {
            if (validateInput()) {
                registerUser();
            }
        });

        btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, WelcomeActivity.class);
            startActivity(intent);
            finish(); // Close this activity to prevent it from staying in the background
        });

        cbOrgMember.setVisibility(rbStudent.isChecked() ? View.VISIBLE : View.GONE);
        layoutOrgPosition.setVisibility(View.GONE);
    }

    private void setupOrgPositionSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new String[]{"Chairperson", "Vice-Chairperson (Internal)", "Vice-Chairperson (External)" , "Secretary", "Associate Secretary", "Treasurer", "Associate Treasurer", "Auditor"}
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spOrgPosition.setAdapter(adapter);
    }

    private boolean validateInput() {
        if (etIdNumber.getText().toString().trim().isEmpty()) {
            showToast("Please enter your ID Number"); return false;
        }
        if (etFirstName.getText().toString().trim().isEmpty()) {
            showToast("Please enter your First Name"); return false;
        }
        if (etLastName.getText().toString().trim().isEmpty()) {
            showToast("Please enter your Last Name"); return false;
        }
        if (etEmail.getText().toString().trim().isEmpty() ||
                !android.util.Patterns.EMAIL_ADDRESS.matcher(etEmail.getText().toString().trim()).matches()) {
            showToast("Enter a valid Email"); return false;
        }
        if (etPassword.getText().toString().length() < 6) {
            showToast("Password must be at least 6 characters"); return false;
        }
        if (!etPassword.getText().toString().equals(etRepeatPassword.getText().toString())) {
            showToast("Passwords do not match"); return false;
        }
        if (rgRole.getCheckedRadioButtonId() == -1) {
            showToast("Select a role"); return false;
        }
        return true;
    }

    private void registerUser() {
        String idNumber = etIdNumber.getText().toString().trim();
        String fullName = etFirstName.getText().toString().trim() + " " +
                etMiddleName.getText().toString().trim() + " " +
                etLastName.getText().toString().trim();
        String email    = etEmail.getText().toString().trim();
        String pwd      = etPassword.getText().toString();
        String role     = ((RadioButton)findViewById(rgRole.getCheckedRadioButtonId())).getText().toString();
        boolean isStudent = role.equals("Student");
        boolean isFaculty = role.equals("Faculty");
        int hasOrg = (isStudent && cbOrgMember.isChecked()) ? 1 : 0;
        String orgPos = (hasOrg == 1) ? spOrgPosition.getSelectedItem().toString() : "";

        try (SQLiteDatabase db = dbHelper.getWritableDatabase()) {
            Cursor c = db.query("users", null, "email=?", new String[]{email}, null, null, null);
            if (c.moveToFirst()) {
                showToast("Email already exists.");
                c.close(); return;
            }
            c.close();

            db.beginTransaction();
            try {
                ContentValues vals = new ContentValues();
                vals.put("name", fullName);
                vals.put("email", email);
                vals.put("password", pwd);
                vals.put("role", role);
                vals.put("has_org", hasOrg);
                vals.put("org_role", orgPos);
                vals.put("verified", 0);

                long userId = db.insert("users", null, vals);
                if (userId != -1) {
                    ContentValues appVals = new ContentValues();
                    appVals.put("student_id", userId);
                    appVals.put("status", "Pending");
                    appVals.put("org_name", isStudent ? "Student Org" : "Faculty");
                    db.insert("applications", null, appVals);

                    ContentValues log = new ContentValues();
                    log.put("user_id", userId);
                    log.put("action_type", "REGISTRATION");
                    log.put("description", "Registered as " + role);
                    log.put("timestamp", System.currentTimeMillis());
                    db.insert("transactions", null, log);

                    db.setTransactionSuccessful();
                    showToast("Registration successful! Await admin verification.");
                    startActivity(new Intent(this, WelcomeActivity.class));
                    finish();
                }
            } finally {
                db.endTransaction();
            }
        } catch (Exception e) {
            showToast("Error: " + e.getMessage());
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) dbHelper.close();
    }
}
