package com.example.cisync.activities;

import android.app.Activity;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import com.example.cisync.R;
import com.example.cisync.database.DBHelper;

public class RegisterActivity extends Activity {

    EditText etName, etEmail, etPassword;
    RadioGroup rgRole;
    CheckBox cbOrgMember;
    Button btnRegister;
    DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        rgRole = findViewById(R.id.rgRole);
        cbOrgMember = findViewById(R.id.cbOrgMember);
        btnRegister = findViewById(R.id.btnRegister);

        dbHelper = new DBHelper(this);

        rgRole.setOnCheckedChangeListener((group, checkedId) -> {
            RadioButton selected = findViewById(checkedId);
            cbOrgMember.setVisibility(selected.getText().toString().equals("Student") ? View.VISIBLE : View.GONE);
        });

        btnRegister.setOnClickListener(v -> registerUser());
    }

    private void registerUser() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        int selectedId = rgRole.getCheckedRadioButtonId();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || selectedId == -1) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
            return;
        }

        RadioButton selected = findViewById(selectedId);
        String role = selected.getText().toString();
        int hasOrg = (role.equals("Student") && cbOrgMember.isChecked()) ? 1 : 0;

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("email", email);
        values.put("password", password);
        values.put("role", role);
        values.put("has_org", hasOrg);

        long result = db.insert("users", null, values);
        if (result != -1) {
            Toast.makeText(this, "Registered successfully!", Toast.LENGTH_SHORT).show();
            finish(); // Go back to Login
        } else {
            Toast.makeText(this, "Registration failed. Email may already exist.", Toast.LENGTH_SHORT).show();
        }
    }
}
