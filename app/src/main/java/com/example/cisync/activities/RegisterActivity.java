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

import java.util.Arrays;
import java.util.List;

public class RegisterActivity extends Activity {
    EditText etIdNumber, etFirstName, etMiddleName, etLastName, etEmail, etPassword, etRepeatPassword;
    RadioGroup rgRole;
    RadioButton rbStudent, rbTeacher;
    CheckBox cbOrgMember;
    Spinner spOrgPosition;
    LinearLayout layoutOrgPosition;
    Button btnRegister, btnBack;
    DBHelper dbHelper;

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

        // findViewById…
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

        // Back arrow/ImageView
        findViewById(R.id.imageView6).setOnClickListener(v -> finish());

        // 1) Role change: show/hide checkbox & position layout
        rgRole.setOnCheckedChangeListener((group, checkedId) -> {
            boolean studentSelected = (checkedId == R.id.rbStudent);
            cbOrgMember.setVisibility(studentSelected ? View.VISIBLE : View.GONE);

            // if switching away, always hide the position block
            if (!studentSelected) {
                layoutOrgPosition.setVisibility(View.GONE);
                cbOrgMember.setChecked(false);
            }
        });

        // 2) Checkbox change: show/hide position only if Student is selected
        cbOrgMember.setOnCheckedChangeListener((button, isChecked) -> {
            if (rbStudent.isChecked()) {
                layoutOrgPosition.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            }
        });

        // Register & Back buttons
        btnRegister.setOnClickListener(v -> registerUser());
        btnBack.setOnClickListener(v -> finish());

        // Initialize visibility based on default selection
        boolean isStudent = rbStudent.isChecked();
        cbOrgMember.setVisibility(isStudent ? View.VISIBLE : View.GONE);
        layoutOrgPosition.setVisibility(View.GONE);
    }

    private void setupOrgPositionSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                ORG_POSITIONS
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spOrgPosition.setAdapter(adapter);
    }

    private void registerUser() {
        String idNumber = etIdNumber.getText().toString().trim();
        String firstName= etFirstName.getText().toString().trim();
        String middle  = etMiddleName.getText().toString().trim();
        String lastName= etLastName.getText().toString().trim();
        String fullName= firstName +
                (middle.isEmpty() ? "" : " " + middle) +
                " " + lastName;
        String email   = etEmail.getText().toString().trim();
        String pwd     = etPassword.getText().toString().trim();
        String rptPwd  = etRepeatPassword.getText().toString().trim();
        int checkedId  = rgRole.getCheckedRadioButtonId();

        if (idNumber.isEmpty() || firstName.isEmpty() || lastName.isEmpty()
                || email.isEmpty() || pwd.isEmpty() || rptPwd.isEmpty()
                || checkedId == -1) {
            Toast.makeText(this, "Please complete all required fields", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!pwd.equals(rptPwd)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        String role = ((RadioButton)findViewById(checkedId)).getText().toString();
        int hasOrg = (rbStudent.isChecked() && cbOrgMember.isChecked()) ? 1 : 0;
        String orgPos = (hasOrg == 1) ? spOrgPosition.getSelectedItem().toString() : "";

        try (SQLiteDatabase db = dbHelper.getWritableDatabase()) {
            // check email
            Cursor c = db.query("users", null, "email=?", new String[]{email}, null, null, null);
            if (c.moveToFirst()) {
                Toast.makeText(this, "Email already exists.", Toast.LENGTH_SHORT).show();
                c.close();
                return;
            }
            c.close();

            ContentValues vals = new ContentValues();
            vals.put("name", fullName);
            vals.put("email", email);
            vals.put("password", pwd);
            vals.put("role", role);
            vals.put("has_org", hasOrg);
            vals.put("org_role", orgPos);
            vals.put("verified", 1);

            long userId = db.insert("users", null, vals);
            if (userId != -1) {
                Toast.makeText(this,
                        "Registration successful! Await admin verification.",
                        Toast.LENGTH_LONG).show();
                logRegistration(db, userId, fullName, role, hasOrg, orgPos);
                startActivity(new Intent(this, WelcomeActivity.class));
                finish();
            } else {
                Toast.makeText(this, "Registration failed.", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private void logRegistration(SQLiteDatabase db, long userId,
                                 String name, String role, int hasOrg, String orgPos) {
        try {
            ContentValues v = new ContentValues();
            v.put("user_id", userId);
            v.put("action_type", "REGISTRATION");
            v.put("description", "New user: " + name + " (" + role + ")" +
                    (hasOrg == 1 ? " - Org: " + orgPos : ""));
            v.put("timestamp", System.currentTimeMillis());
            db.insert("transactions", null, v);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // nothing extra needed here for org fields
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) dbHelper.close();
    }
}
