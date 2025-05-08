package com.example.cisync;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Redirect to Welcome Activity
        startActivity(new Intent(this, com.example.cisync.activities.WelcomeActivity.class));
        finish(); // Prevent returning to MainActivity
    }
}
