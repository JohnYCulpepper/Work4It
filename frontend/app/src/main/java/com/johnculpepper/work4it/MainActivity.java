package com.johnculpepper.work4it;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.content.Intent;

import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import android.app.AppOpsManager;
import android.view.accessibility.AccessibilityManager;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.provider.Settings;
import java.util.List;

import java.time.LocalDate;
import android.os.Handler;
import android.os.Looper;


public class MainActivity extends AppCompatActivity {
    private TimeManager timeManager;
    private TextView timeDisplay;
    private final Handler permissionHandler = new Handler(Looper.getMainLooper());
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

    // Stops the activity from continuing to run while already on another activity
    @Override
    protected void onDestroy() {
        super.onDestroy();
        permissionHandler.removeCallbacksAndMessages(null);
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
    private void nextDayChecker() {
        String lastCheckedStr = prefs.getString("last_checked_date", LocalDate.now().toString());
        LocalDate lastChecked = LocalDate.parse(lastCheckedStr);
        LocalDate today = LocalDate.now();

        if (today.isAfter(lastChecked)) {
            timeManager.resetDaily();
            prefs.edit().putString("last_checked_date", today.toString()).apply();
        }
    }
    // Safety net: checks whether it's okay for the app to continue
    private boolean pickAppToBlock() {
        boolean setupDone = prefs.getBoolean("pickAppSetupDone", false);
        if (!setupDone) {
            Intent intent = new Intent(MainActivity.this, PickAppsToBlock.class);
            startActivity(intent);
            Log.d("MainActivity/Redirected", "Successfully redirected to PickAppsToBlock");
            return true;
        }
        return false;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        prefs = getSharedPreferences("Work4It", MODE_PRIVATE);

        timeManager = new TimeManager(this);
        timeManager.addMinutes(115);
        timeDisplay = findViewById(R.id.timeDisplay);
        nextDayChecker();

        // Stops immediately if redirected to PickAppsToBlock
        if (pickAppToBlock()) return;
        prefs.edit().putBoolean("get_requested", true).apply();

        checkPermissions();
        updateTimeDisplay();

        Button exerciseButton = findViewById(R.id.exerciseButton);
        exerciseButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ExerciseActivity.class);
            startActivity(intent);
        });
        Button pickAppsButton = findViewById(R.id.pickAppsButton);
        pickAppsButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, PickAppsToBlock.class);
            startActivity(intent);
        });
        Button devTestReset = findViewById(R.id.devTestReset);
        devTestReset.setOnClickListener(v -> {
            timeManager.resetDaily();
            Log.d("devTest:TimeReset", "Time reset back to 0");
            timeDisplay.setText("0 hr 00 min 00 sec remaining");
        });
    }
    @Override
    protected void onResume() {
        super.onResume();
        nextDayChecker();
        checkPermissions();

        if (pickAppToBlock()) return;

        updateTimeDisplay();
    }

    private void updateTimeDisplay() {
        int[] time = timeManager.getTimeRemaining();
        String hour = Integer.toString(time[2]);
        String min = Integer.toString(time[1]);
        String sec = Integer.toString(time[0]);
        if (min.length() == 1) { min = "0" + min; }
        if (sec.length() == 1) { sec = "0" + sec; }

        String timeDisplayStr = hour + " hr " + min + " min " + sec + " sec remaining";
        timeDisplay.setText(timeDisplayStr);
    }
}