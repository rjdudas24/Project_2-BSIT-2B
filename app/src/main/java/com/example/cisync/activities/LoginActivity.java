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
    Button btnLogin;
    ImageButton btnGoBack;
    DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etLoginEmail);
        etPassword = findViewById(R.id.etLoginPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoBack = findViewById(R.id.btnGoBack);

        dbHelper = new DBHelper(this);

        btnLogin.setOnClickListener(v -> loginUser());
        btnGoBack.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, WelcomeActivity.class);
            startActivity(intent);
            finish(); // Close this activity to prevent it from staying in the background
        });
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Enter email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        SQLiteDatabase db = dbHelper.getReadableDatabase();



        // Add verification check to the query
        Cursor cursor = db.rawQuery(
                "SELECT * FROM users WHERE email=? AND password=?",
                new String[]{email, password}
        );

        if (cursor.moveToFirst()) {
            int userId = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
            String role = cursor.getString(cursor.getColumnIndexOrThrow("role"));
            int verifiedStatus = 1; // Default to verified for backward compatibility

            // Check if verified column exists and get its value
            int verifiedColumnIndex = cursor.getColumnIndex("verified");
            if (verifiedColumnIndex != -1) {
                verifiedStatus = cursor.getInt(verifiedColumnIndex);
            }

            // Check if user is verified
            if (verifiedStatus == 0) {
                Toast.makeText(this, "Your account is pending verification by an administrator.",
                        Toast.LENGTH_LONG).show();
                cursor.close();
                db.close();
                return;
            }

            // Get additional user data
            boolean hasOrg = false;
            int hasOrgColumnIndex = cursor.getColumnIndex("has_org");
            if (hasOrgColumnIndex != -1) {
                hasOrg = cursor.getInt(hasOrgColumnIndex) == 1;
            }

            Intent intent;
            if (role.equals("Student")) {
                intent = new Intent(this, DashboardStudentActivity.class);
                intent.putExtra("hasOrg", hasOrg);
                intent.putExtra("studentId", userId);
            } else if (role.equals("Faculty")) {
                intent = new Intent(this, DashboardFacultyActivity.class);
            } else if (role.equals("Admin")) { // Explicit check for Admin
                intent = new Intent(this, DashboardAdminActivity.class);
            } else { // Any other role as fallback
                Toast.makeText(this, "Unknown role: " + role, Toast.LENGTH_SHORT).show();
                return; // Don't proceed with invalid role
            }


            // Add user ID to any intent for consistency
            intent.putExtra("userId", userId);

            startActivity(intent);
            finish(); // Close login activity to prevent it from staying in background
        } else {
            Toast.makeText(this, "Invalid login credentials", Toast.LENGTH_SHORT).show();
        }
        cursor.close();
        db.close();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}