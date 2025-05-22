package com.example.cisync.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.util.Log;
import android.widget.Button;
import com.example.cisync.R;

/**
 * Utility class to handle logout functionality across all dashboard activities.
 * This provides a consistent logout experience with minimal code duplication.
 */
public class LogoutUtil {

    private static final String TAG = "LogoutUtil";

    /**
     * Shows logout confirmation dialog with custom styling
     * @param activity The current activity (Dashboard activity)
     * @param userId The ID of the logged-in user
     */
    public static void showLogoutDialog(Activity activity, int userId) {
        AlertDialog dialog = new AlertDialog.Builder(activity, R.style.CustomLogoutDialogTheme)
                .setTitle("Confirm Logout")
                .setMessage("Are you sure you want to logout from your admin session?")
                .setPositiveButton("Logout", (dialogInterface, which) -> performLogout(activity, userId))
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_lock_power_off) // Using built-in icon
                .setCancelable(true)
                .create();

        // Additional customization after dialog is created
        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

            // Customize button colors
            if (positiveButton != null) {
                positiveButton.setTextColor(activity.getResources().getColor(android.R.color.holo_red_dark, null));
                positiveButton.setAllCaps(false);
            }
            if (negativeButton != null) {
                negativeButton.setTextColor(activity.getResources().getColor(R.color.darker_blue, null));
                negativeButton.setAllCaps(false);
            }
        });

        dialog.show();
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