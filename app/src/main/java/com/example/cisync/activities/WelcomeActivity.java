package com.example.cisync.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import com.example.cisync.R;

public class WelcomeActivity extends Activity {
    private static final int SPLASH_DELAY = 2000; // 2 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        LinearLayout llButtons = findViewById(R.id.llButtons);
        Button btnLogin = findViewById(R.id.btnLoginWelcome);
        Button btnRegister = findViewById(R.id.btnRegisterWelcome);

        // Hide buttons initially during splash
        llButtons.setVisibility(View.GONE);

        // After SPLASH_DELAY, show buttons and wire up click listeners
        new Handler().postDelayed(() -> {
            llButtons.setVisibility(View.VISIBLE);

            btnLogin.setOnClickListener(v -> {
                startActivity(new Intent(WelcomeActivity.this, LoginActivity.class));
                finish();
            });

            btnRegister.setOnClickListener(v -> {
                startActivity(new Intent(WelcomeActivity.this, RegisterActivity.class));
                finish();
            });
        }, SPLASH_DELAY);
    }
}
