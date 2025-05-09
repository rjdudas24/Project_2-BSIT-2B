package com.example.cisync.activities;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.*;
import com.example.cisync.R;
import com.example.cisync.database.DBHelper;

public class LoginActivity extends Activity {

    EditText etEmail, etPassword;
    Button btnLogin, btnGoRegister;
    DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etLoginEmail);
        etPassword = findViewById(R.id.etLoginPassword);
        btnLogin = findViewById(R.id.btnLogin);
        ImageButton btnGoBack = findViewById(R.id.btnGoBack);

        dbHelper = new DBHelper(this);

        btnLogin.setOnClickListener(v -> loginUser());
        btnGoBack.setOnClickListener(v ->
                startActivity(new Intent(this, WelcomeActivity.class))
        );
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Enter email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM users WHERE email=? AND password=?", new String[]{email, password});

        if (cursor.moveToFirst()) {
            String role = cursor.getString(cursor.getColumnIndexOrThrow("role"));
            boolean hasOrg = cursor.getInt(cursor.getColumnIndexOrThrow("has_org")) == 1;

            Intent intent;
            if (role.equals("Student")) {
                intent = new Intent(this, DashboardStudentActivity.class);
                intent.putExtra("hasOrg", hasOrg);
            } else if (role.equals("Faculty")) {
                intent = new Intent(this, DashboardFacultyActivity.class);
            } else {
                intent = new Intent(this, DashboardAdminActivity.class);
            }

            startActivity(intent);
            finish();
        } else {
            Toast.makeText(this, "Invalid login credentials", Toast.LENGTH_SHORT).show();
        }
        cursor.close();
        db.close();
    }
}
