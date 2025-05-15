package com.example.cisync.activities;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.widget.*;
import com.example.cisync.R;
import com.example.cisync.database.DBHelper;

public class LoginActivity extends Activity {

    private static final String TAG = "LoginActivity";
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

        try {
            SQLiteDatabase db = dbHelper.getReadableDatabase();

            // First query to check if user exists
            Cursor cursor = db.rawQuery(
                    "SELECT * FROM users WHERE email=? AND password=?",
                    new String[]{email, password}
            );

            if (cursor.moveToFirst()) {
                // User exists, proceed with login
                int userId = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                String role = cursor.getString(cursor.getColumnIndexOrThrow("role"));

                // Add debug info
                Log.d(TAG, "User found: ID=" + userId + ", Name=" + name + ", Role=" + role);

                // Check verification status with safe default
                int verifiedStatus = 1; // Default to verified
                int verifiedColumnIndex = cursor.getColumnIndex("verified");
                if (verifiedColumnIndex != -1) {
                    verifiedStatus = cursor.getInt(verifiedColumnIndex);
                    Log.d(TAG, "Verification status: " + verifiedStatus);
                } else {
                    Log.d(TAG, "Verified column not found in result");
                }

                // If not verified, show message and return
                if (verifiedStatus == 0) {
                    Toast.makeText(this, "Your account is pending verification by an administrator.",
                            Toast.LENGTH_LONG).show();
                    cursor.close();
                    db.close();
                    return;
                }

                // Safely check if user has an organization
                boolean hasOrg = false;
                int hasOrgColumnIndex = cursor.getColumnIndex("has_org");
                if (hasOrgColumnIndex != -1) {
                    hasOrg = cursor.getInt(hasOrgColumnIndex) == 1;
                    Log.d(TAG, "Has organization: " + hasOrg);
                } else {
                    Log.d(TAG, "has_org column not found in result");
                }

                // Proceed to appropriate dashboard based on role
                Intent intent;
                if ("Student".equals(role)) {
                    if (hasOrg) {
                        Log.d(TAG, "Routing to Student with Org dashboard");
                        intent = new Intent(this, DashboardStudentwOrgActivity.class);
                        intent.putExtra("hasOrg", true);
                    } else {
                        Log.d(TAG, "Routing to Student without Org dashboard");
                        intent = new Intent(this, DashboardStudentActivity.class);
                    }
                    intent.putExtra("studentId", userId);
                } else if ("Faculty".equals(role)) {
                    Log.d(TAG, "Routing to Faculty dashboard");
                    intent = new Intent(this, DashboardFacultyActivity.class);
                } else if ("Admin".equals(role)) {
                    Log.d(TAG, "Routing to Admin dashboard");
                    intent = new Intent(this, DashboardAdminActivity.class);
                } else {
                    Log.d(TAG, "Unknown role: " + role);
                    Toast.makeText(this, "Unknown role: " + role, Toast.LENGTH_SHORT).show();
                    cursor.close();
                    db.close();
                    return;
                }

                // Add user ID to intent and start activity
                intent.putExtra("userId", userId);
                startActivity(intent);
                finish(); // Close login activity
            } else {
                // User not found or invalid credentials
                Toast.makeText(this, "Invalid login credentials", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Invalid login attempt for email: " + email);
            }
            cursor.close();
            db.close();
        } catch (Exception e) {
            // Handle any exceptions that might occur
            Log.e(TAG, "Error during login process", e);
            Toast.makeText(this, "Error during login: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}