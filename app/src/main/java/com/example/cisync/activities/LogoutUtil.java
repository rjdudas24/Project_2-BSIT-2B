package com.example.cisync.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.util.Log;

/**
 * Utility class to handle logout functionality across all dashboard activities.
 * This provides a consistent logout experience with minimal code duplication.
 */
public class LogoutUtil {

    private static final String TAG = "LogoutUtil";

    /**
     * Shows logout confirmation dialog and handles the logout process
     * @param activity The current activity (Dashboard activity)
     * @param userId The ID of the logged-in user
     */
    public static void showLogoutDialog(Activity activity, int userId) {
        new AlertDialog.Builder(activity)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> performLogout(activity, userId))
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    /**
     * Performs the actual logout process
     * @param activity The current activity
     * @param userId The ID of the logged-in user
     */
    private static void performLogout(Activity activity, int userId) {
        try {
            // Record logout session if user ID is valid
            if (userId != -1) {
                LoginActivity.recordLogoutSession(activity, userId);
                Log.d(TAG, "Logout recorded for user ID: " + userId);
            }

            // Navigate back to welcome screen and clear the activity stack
            Intent intent = new Intent(activity, WelcomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            activity.startActivity(intent);
            activity.finish();

            Log.d(TAG, "Logout completed successfully");

        } catch (Exception e) {
            Log.e(TAG, "Error during logout: " + e.getMessage(), e);
            // Still navigate to welcome screen even if logout recording fails
            Intent intent = new Intent(activity, WelcomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            activity.startActivity(intent);
            activity.finish();
        }
    }

    /**
     * Quick logout without confirmation dialog (for emergency cases)
     * @param activity The current activity
     * @param userId The ID of the logged-in user
     */
    public static void quickLogout(Activity activity, int userId) {
        performLogout(activity, userId);
    }
}