package com.example.cisync.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;
import com.example.cisync.R;
import com.example.cisync.database.DBHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class AdminLoginHistoryActivity extends Activity {

    private static final String TAG = "AdminLoginHistory";
    ListView lvLoginHistory;
    TextView tvNoLogins;
    Spinner spinnerRoleFilter;
    Button btnApplyFilter, btnClearFilter;
    ImageView btnBackLoginHistory;

    DBHelper dbHelper;
    ArrayList<String> loginList = new ArrayList<>();
    ArrayList<LoginData> loginHistoryList = new ArrayList<>();
    ArrayAdapter<String> adapter;
    String currentFilter = "All"; // Default filter

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_login_history);

        // Initialize views
        initializeViews();

        // Set up filter spinner
        setupFilterSpinner();

        // Set up click listeners
        setupClickListeners();

        // Load initial data
        loadLoginHistory(null);
    }

    private void initializeViews() {
        try {
            lvLoginHistory = findViewById(R.id.lvLoginHistory);
            tvNoLogins = findViewById(R.id.tvNoLogins);
            spinnerRoleFilter = findViewById(R.id.spinnerRoleFilter);
            btnApplyFilter = findViewById(R.id.btnApplyFilter);
            btnClearFilter = findViewById(R.id.btnClearFilter);
            btnBackLoginHistory = findViewById(R.id.btnBackLoginHistory);
            dbHelper = new DBHelper(this);

            // Initialize adapter with custom layout - will be set up in loadLoginHistory()
            adapter = new ArrayAdapter<>(this, R.layout.custom_login_history_item, loginList);
            lvLoginHistory.setAdapter(adapter);
        } catch (Exception e) {
            Log.e(TAG, "Error initializing views: " + e.getMessage(), e);
            Toast.makeText(this, "Error initializing views", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupFilterSpinner() {
        try {
            // Create filter options
            ArrayList<String> filterOptions = new ArrayList<>();
            filterOptions.add("All");
            filterOptions.add("Admin");
            filterOptions.add("Faculty");
            filterOptions.add("Student");

            // Set up spinner adapter
            ArrayAdapter<String> filterAdapter = new ArrayAdapter<>(
                    this, R.layout.custom_role_spinner, filterOptions);
            filterAdapter.setDropDownViewResource(R.layout.custom_role_spinner);
            spinnerRoleFilter.setAdapter(filterAdapter);
        } catch (Exception e) {
            Log.e(TAG, "Error setting up filter spinner: " + e.getMessage(), e);
        }
    }

    private void setupClickListeners() {
        try {
            // Back button
            btnBackLoginHistory.setOnClickListener(v -> finish());

            // Apply filter button
            btnApplyFilter.setOnClickListener(v -> {
                currentFilter = spinnerRoleFilter.getSelectedItem().toString();
                if (currentFilter.equals("All")) {
                    loadLoginHistory(null);
                } else {
                    loadLoginHistory(currentFilter);
                }
            });

            // Clear filter button
            btnClearFilter.setOnClickListener(v -> {
                spinnerRoleFilter.setSelection(0); // Set to "All"
                currentFilter = "All";
                loadLoginHistory(null);
            });

            // List item click listener for viewing details
            lvLoginHistory.setOnItemClickListener((parent, view, position, id) -> {
                if (position < loginHistoryList.size()) {
                    LoginData login = loginHistoryList.get(position);
                    showLoginDetailsDialog(login);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error setting up click listeners: " + e.getMessage(), e);
        }
    }

    private void loadLoginHistory(String roleFilter) {
        loginList.clear();
        loginHistoryList.clear();

        SQLiteDatabase db = dbHelper.getReadableDatabase();

        try {
            String query;
            String[] selectionArgs;

            // Check if login_history table exists
            Cursor checkTable = db.rawQuery(
                    "SELECT name FROM sqlite_master WHERE type='table' AND name='login_history'",
                    null);

            // If table doesn't exist, create it
            if (checkTable.getCount() == 0) {
                checkTable.close();
                createLoginHistoryTable(db);
                // Add sample data for testing
                insertSampleLoginData(db);
            } else {
                checkTable.close();
            }

            if (roleFilter == null || roleFilter.equals("All")) {
                // No filter - get all login history
                query = "SELECT l.id, l.user_id, u.name, u.role, l.login_time, l.logout_time, l.device_info " +
                        "FROM login_history l " +
                        "JOIN users u ON l.user_id = u.id " +
                        "ORDER BY l.login_time DESC";
                selectionArgs = null;
            } else {
                // Apply filter by role
                query = "SELECT l.id, l.user_id, u.name, u.role, l.login_time, l.logout_time, l.device_info " +
                        "FROM login_history l " +
                        "JOIN users u ON l.user_id = u.id " +
                        "WHERE u.role = ? " +
                        "ORDER BY l.login_time DESC";
                selectionArgs = new String[]{roleFilter};
            }

            Cursor cursor = db.rawQuery(query, selectionArgs);

            if (cursor.moveToFirst() && cursor.getCount() > 0) {
                // Hide empty message, show list
                tvNoLogins.setVisibility(View.GONE);
                lvLoginHistory.setVisibility(View.VISIBLE);

                do {
                    int id = cursor.getInt(0);
                    int userId = cursor.getInt(1);
                    String name = cursor.getString(2);
                    String role = cursor.getString(3);
                    long loginTime = cursor.getLong(4);
                    long logoutTime = cursor.getLong(5);
                    String deviceInfo = cursor.getString(6);

                    // Create login data object
                    LoginData login = new LoginData(
                            id, userId, name, role, loginTime, logoutTime, deviceInfo);
                    loginHistoryList.add(login);

                    // Format for display
                    String formattedLoginTime = formatTimestamp(loginTime);
                    String formattedLogoutTime = logoutTime > 0 ? formatTimestamp(logoutTime) : "Session Active";

                    // Create display text
                    String displayText = "\n" + formattedLoginTime + "\n" +
                            name + " (" + role + ")" + "\n";

                    loginList.add(displayText);
                } while (cursor.moveToNext());

            } else {
                // No login history found - show message
                tvNoLogins.setVisibility(View.VISIBLE);
                lvLoginHistory.setVisibility(View.GONE);

                tvNoLogins.setText("No login history found" +
                        (roleFilter != null && !roleFilter.equals("All") ? " for " + roleFilter + " users" : ""));
            }

            cursor.close();

            // Update adapter
            adapter = new ArrayAdapter<>(this, R.layout.custom_login_history_item, loginList);
            lvLoginHistory.setAdapter(adapter);

        } catch (Exception e) {
            Log.e(TAG, "Error loading login history: " + e.getMessage(), e);
            Toast.makeText(this, "Error loading login history: " + e.getMessage(), Toast.LENGTH_SHORT).show();

            // Show error state
            tvNoLogins.setVisibility(View.VISIBLE);
            tvNoLogins.setText("Error loading login history");
            lvLoginHistory.setVisibility(View.GONE);
        }
    }

    private void createLoginHistoryTable(SQLiteDatabase db) {
        try {
            db.execSQL("CREATE TABLE IF NOT EXISTS login_history (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "user_id INTEGER, " +
                    "login_time INTEGER, " +
                    "logout_time INTEGER, " +
                    "device_info TEXT, " +
                    "FOREIGN KEY (user_id) REFERENCES users(id))");

            Log.d(TAG, "login_history table created successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error creating login_history table: " + e.getMessage(), e);
        }
    }

    private void insertSampleLoginData(SQLiteDatabase db) {
        try {
            // Get some user IDs from the users table
            Cursor cursor = db.rawQuery(
                    "SELECT id, role FROM users LIMIT 10",
                    null);

            if (cursor.moveToFirst()) {
                long currentTime = System.currentTimeMillis();
                long oneHour = 60 * 60 * 1000;
                long oneDay = 24 * oneHour;

                do {
                    int userId = cursor.getInt(0);
                    String role = cursor.getString(1);

                    // Insert multiple login entries for this user
                    for (int i = 0; i < 3; i++) {
                        long loginTime = currentTime - (i * oneDay);
                        long logoutTime = loginTime + oneHour + (int)(Math.random() * oneHour);

                        // Some entries will have active sessions (no logout time)
                        if (i == 0 && Math.random() < 0.3) {
                            logoutTime = 0;
                        }

                        String deviceInfo = "Android " + (10 + (int)(Math.random() * 3)) +
                                " / " + (role.equals("Admin") ? "Desktop" : "Mobile");

                        db.execSQL(
                                "INSERT INTO login_history (user_id, login_time, logout_time, device_info) VALUES (?, ?, ?, ?)",
                                new Object[]{userId, loginTime, logoutTime, deviceInfo}
                        );
                    }

                } while (cursor.moveToNext());
            }

            cursor.close();
            Log.d(TAG, "Sample login data inserted successfully");

        } catch (Exception e) {
            Log.e(TAG, "Error inserting sample login data: " + e.getMessage(), e);
        }
    }

    private String formatTimestamp(long timestamp) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        } catch (Exception e) {
            return "Unknown date";
        }
    }

    private String calculateSessionDuration(long loginTime, long logoutTime) {
        try {
            if (logoutTime <= loginTime) {
                return "Invalid session";
            }

            long durationMillis = logoutTime - loginTime;
            long seconds = durationMillis / 1000;
            long minutes = seconds / 60;
            long hours = minutes / 60;
            long days = hours / 24;

            if (days > 0) {
                return days + "d " + (hours % 24) + "h " + (minutes % 60) + "m";
            } else if (hours > 0) {
                return hours + "h " + (minutes % 60) + "m";
            } else if (minutes > 0) {
                return minutes + "m " + (seconds % 60) + "s";
            } else {
                return seconds + "s";
            }
        } catch (Exception e) {
            return "Unknown duration";
        }
    }

    private void showLoginDetailsDialog(LoginData login) {
        try {
            // Create custom dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            // Inflate custom layout
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_login_details, null);
            builder.setView(dialogView);

            // Create dialog
            AlertDialog dialog = builder.create();

            // Make dialog background transparent to show custom background
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }

            // Find views in custom layout
            TextView tvUserName = dialogView.findViewById(R.id.tvDialogUserName);
            TextView tvUserRole = dialogView.findViewById(R.id.tvDialogUserRole);
            TextView tvUserId = dialogView.findViewById(R.id.tvDialogUserId);
            TextView tvLoginTime = dialogView.findViewById(R.id.tvDialogLoginTime);
            TextView tvLogoutTime = dialogView.findViewById(R.id.tvDialogLogoutTime);
            TextView tvDuration = dialogView.findViewById(R.id.tvDialogDuration);
            TextView tvSessionStatus = dialogView.findViewById(R.id.tvDialogSessionStatus);
            TextView tvDeviceInfo = dialogView.findViewById(R.id.tvDialogDeviceInfo);
            View vStatusIndicator = dialogView.findViewById(R.id.vSessionStatusIndicator);
            ImageView ivDeviceIcon = dialogView.findViewById(R.id.ivDeviceIcon);
            Button btnDialogClose = dialogView.findViewById(R.id.btnDialogClose);

            // Set user information
            tvUserName.setText(login.getName());
            tvUserRole.setText(login.getRole());
            tvUserId.setText(String.valueOf(login.getUserId()));

            // Set session information
            tvLoginTime.setText(formatTimestamp(login.getLoginTime()));

            boolean isSessionActive = login.getLogoutTime() <= 0;
            if (isSessionActive) {
                tvLogoutTime.setText("Session Active");
                tvDuration.setText("Active");
                tvSessionStatus.setText("Active");
                tvSessionStatus.setTextColor(getResources().getColor(android.R.color.holo_green_light));
                // Set status indicator to green for active sessions
                vStatusIndicator.setBackgroundResource(R.drawable.status_indicator_active);
            } else {
                tvLogoutTime.setText(formatTimestamp(login.getLogoutTime()));
                tvDuration.setText(calculateSessionDuration(login.getLoginTime(), login.getLogoutTime()));
                tvSessionStatus.setText("Ended");
                tvSessionStatus.setTextColor(getResources().getColor(android.R.color.holo_red_light));
                // Set status indicator to red for ended sessions
                vStatusIndicator.setBackgroundResource(R.drawable.status_indicator_ended);
            }

            // Set device information
            tvDeviceInfo.setText(login.getDeviceInfo());

            // Set appropriate device icon based on device info
            if (login.getDeviceInfo().toLowerCase().contains("desktop")) {
                ivDeviceIcon.setImageResource(R.drawable.baseline_computer_24);
            } else if (login.getDeviceInfo().toLowerCase().contains("tablet")) {
                ivDeviceIcon.setImageResource(R.drawable.baseline_tablet_24);
            } else {
                ivDeviceIcon.setImageResource(R.drawable.baseline_phone_android_24);
            }

            // Set role-specific styling
            switch (login.getRole().toLowerCase()) {
                case "admin":
                    tvUserRole.setBackgroundColor(getResources().getColor(R.color.admin_role_color));
                    break;
                case "faculty":
                    tvUserRole.setBackgroundColor(getResources().getColor(R.color.faculty_role_color));
                    break;
                case "student":
                    tvUserRole.setBackgroundColor(getResources().getColor(R.color.student_role_color));
                    break;
                default:
                    tvUserRole.setBackgroundColor(getResources().getColor(R.color.default_role_color));
                    break;
            }

            // Set click listeners for close buttons
            btnDialogClose.setOnClickListener(v -> dialog.dismiss());

            // Show dialog
            dialog.show();

        } catch (Exception e) {
            Log.e(TAG, "Error showing custom login details dialog: " + e.getMessage(), e);

            // Fallback to simple dialog if custom dialog fails
            AlertDialog.Builder fallbackBuilder = new AlertDialog.Builder(this);
            fallbackBuilder.setTitle("Login Details");

            String message = "User: " + login.getName() + "\n" +
                    "Role: " + login.getRole() + "\n" +
                    "User ID: " + login.getUserId() + "\n\n" +
                    "Login Time: " + formatTimestamp(login.getLoginTime()) + "\n" +
                    "Logout Time: " + (login.getLogoutTime() > 0 ? formatTimestamp(login.getLogoutTime()) : "Session Active") + "\n" +
                    "Session Duration: " + (login.getLogoutTime() > 0 ? calculateSessionDuration(login.getLoginTime(), login.getLogoutTime()) : "Active") + "\n\n" +
                    "Device Info: " + login.getDeviceInfo();

            fallbackBuilder.setMessage(message);
            fallbackBuilder.setPositiveButton("Close", null);
            fallbackBuilder.show();

            Toast.makeText(this, "Using fallback dialog due to error", Toast.LENGTH_SHORT).show();
        }
    }

    // Method to record a new login - static so it can be called from LoginActivity
    public static void recordLogin(SQLiteDatabase db, int userId, String deviceInfo) {
        try {
            // Check if table exists
            Cursor checkTable = db.rawQuery(
                    "SELECT name FROM sqlite_master WHERE type='table' AND name='login_history'",
                    null);

            // If table doesn't exist, create it
            if (checkTable.getCount() == 0) {
                db.execSQL("CREATE TABLE IF NOT EXISTS login_history (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "user_id INTEGER, " +
                        "login_time INTEGER, " +
                        "logout_time INTEGER, " +
                        "device_info TEXT, " +
                        "FOREIGN KEY (user_id) REFERENCES users(id))");
            }

            checkTable.close();

            // Insert login record
            db.execSQL(
                    "INSERT INTO login_history (user_id, login_time, logout_time, device_info) VALUES (?, ?, ?, ?)",
                    new Object[]{userId, System.currentTimeMillis(), 0, deviceInfo}
            );

            Log.d(TAG, "Login recorded for user ID: " + userId);
        } catch (Exception e) {
            Log.e(TAG, "Error recording login: " + e.getMessage(), e);
        }
    }

    // Method to record a logout - static so it can be called from anywhere
    public static void recordLogout(SQLiteDatabase db, int userId) {
        try {
            // Update the most recent login record for this user
            db.execSQL(
                    "UPDATE login_history SET logout_time = ? " +
                            "WHERE user_id = ? AND logout_time = 0 " +
                            "ORDER BY login_time DESC LIMIT 1",
                    new Object[]{System.currentTimeMillis(), userId}
            );

            Log.d(TAG, "Logout recorded for user ID: " + userId);
        } catch (Exception e) {
            Log.e(TAG, "Error recording logout: " + e.getMessage(), e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }

    // Login data class
    private static class LoginData {
        private final int id;
        private final int userId;
        private final String name;
        private final String role;
        private final long loginTime;
        private final long logoutTime;
        private final String deviceInfo;

        public LoginData(int id, int userId, String name, String role, long loginTime, long logoutTime, String deviceInfo) {
            this.id = id;
            this.userId = userId;
            this.name = name != null ? name : "Unknown";
            this.role = role != null ? role : "Unknown";
            this.loginTime = loginTime;
            this.logoutTime = logoutTime;
            this.deviceInfo = deviceInfo != null ? deviceInfo : "Unknown";
        }

        public int getId() { return id; }
        public int getUserId() { return userId; }
        public String getName() { return name; }
        public String getRole() { return role; }
        public long getLoginTime() { return loginTime; }
        public long getLogoutTime() { return logoutTime; }
        public String getDeviceInfo() { return deviceInfo; }
    }
}