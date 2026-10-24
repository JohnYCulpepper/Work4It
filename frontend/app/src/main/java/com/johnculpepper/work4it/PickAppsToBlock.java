package com.johnculpepper.work4it;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class PickAppsToBlock extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_picker);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.picker), (v, insets) -> { // fixed: was R.id.main
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        SlideButton sliderInstagram = findViewById(R.id.slideButtonInsta);
        sliderInstagram.setupClickListener("com.instagram.android");

        SlideButton sliderTiktok = findViewById(R.id.slideButtonTikTok);
        sliderTiktok.setupClickListener("com.zhiliaoapp.musically");

        SlideButton sliderYoutube = findViewById(R.id.slideButtonYT);
        sliderYoutube.setupClickListener("com.google.android.youtube");

        SlideButton sliderFacebook = findViewById(R.id.slideButtonFB);
        sliderFacebook.setupClickListener("com.facebook.katana");

        Button continueToMain = findViewById(R.id.continueToHome);
        continueToMain.setOnClickListener(v -> {
            // Mark setup as complete so MainActivity stops redirecting here
            SharedPreferences prefs = getSharedPreferences("Work4It", MODE_PRIVATE);
            prefs.edit().putBoolean("pickAppSetupDone", true).apply();

            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        });
    }
}