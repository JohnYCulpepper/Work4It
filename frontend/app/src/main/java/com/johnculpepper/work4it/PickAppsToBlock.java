package com.johnculpepper.work4it;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.List;

public class PickAppsToBlock extends AppCompatActivity {
    private SharedPreferences prefs;
    private boolean isAccessibilityEnabled() {
        Log.d("isAccessibilityEnabled", "successful run");
        AccessibilityManager am = (AccessibilityManager) getSystemService(ACCESSIBILITY_SERVICE);
        List<AccessibilityServiceInfo> enabledServices =
                am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
        for (AccessibilityServiceInfo service : enabledServices) {
            if (service.getId().contains(getPackageName())) {
                return true;
            }
        }
        return false;
    }
    private boolean isUsageAccessEnabled() {
        AppOpsManager appOps = (AppOpsManager) getSystemService(APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }
    private boolean isOverlayEnabled() {
        return Settings.canDrawOverlays(this);
    }

    private void checkPermissions() {
        boolean permissionsRequested = prefs.getBoolean("just_requested", false);

        if (permissionsRequested) {
            prefs.edit().putBoolean("get_requested", false).apply();
            return ;
        }
        if (!isAccessibilityEnabled()) {
            new AlertDialog.Builder(this)
                    .setTitle("Enable Accessibility")
                    .setMessage("Work4It needs Accessibility permission to block apps, which allows the app to work as intended. " +
                            "Tap OK to enable it.")
                    .setPositiveButton("OK", (d, w) -> {
                        startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        } else if (!isUsageAccessEnabled()) {
            new AlertDialog.Builder(this)
                    .setTitle("Enable Usage Access")
                    .setMessage("Work4It needs Usage Access to track screen time. Tap OK to enable it.")
                    .setPositiveButton("OK", (d, w) -> {
                        startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        } else if (!isOverlayEnabled()) {
            new AlertDialog.Builder(this)
                    .setTitle("Enable Display Over Apps")
                    .setMessage("Work4It needs to display over other apps to block them. Tap OK to enable it.")
                    .setPositiveButton("OK", (d, w) -> {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:" + getPackageName()));
                        startActivity(intent);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }
    }
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
        prefs = getSharedPreferences("Work4It", MODE_PRIVATE);

        checkPermissions();

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