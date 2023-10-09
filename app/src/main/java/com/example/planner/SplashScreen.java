package com.example.planner;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

public class SplashScreen extends AppCompatActivity {
    Button registerBtn;
    TextView loginBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AppSharedPreferences sharedPreferences = new AppSharedPreferences(this);

        //getting user state
        System.out.println(sharedPreferences.isLoggedIn());
        if (sharedPreferences.isLoggedIn()) {
            // Do something for the logged user; open AllActivity
            Intent intent = new Intent(this, DashboardActivity.class);
            startActivity(intent);
            finish();
        } else {
            // Do something for the unlogged user
            setContentView(R.layout.activity_splash_screen);

            // This is used to hide the status bar and make
            // the splash screen as a full screen activity.
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

            ConstraintLayout splashScreen = findViewById(R.id.splashScreen);
            Animation animation = AnimationUtils.loadAnimation(this, R.anim.animation);
            splashScreen.startAnimation(animation);

            registerBtn = findViewById(R.id.registerBtn);
            loginBtn = findViewById(R.id.loginBtn);

            registerBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // redirect to RegisterActivity
                    Intent intent = new Intent(getApplicationContext(), RegisterActivity.class);
                    startActivity(intent);
                }
            });

            loginBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // redirect to RegisterActivity
                    Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                    startActivity(intent);
                }
            });
        }
    }
}
