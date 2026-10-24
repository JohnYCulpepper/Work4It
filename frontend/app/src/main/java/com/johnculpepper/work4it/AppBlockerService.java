package com.johnculpepper.work4it;

import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.CountDownTimer;
import android.view.accessibility.AccessibilityEvent;
import android.util.Log;

import java.util.HashMap;



@SuppressLint("AccessibilityPolicy")
public class AppBlockerService extends AccessibilityService {
    private TimeManager timeManager;
    private CountDownTimer countDownTimer;
    protected HashMap<String, Boolean> blockedApps = new HashMap<>();

    private final String[] packageNames = {
            "com.instagram.android",
            "com.zhiliaoapp.musically",
            "com.google.android.youtube",
            "com.facebook.katana"
    };
    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        timeManager = new TimeManager(this);
    }

    // Checks whether the app currently open is blocked
    public boolean isBlocked(CharSequence packageName) {
        SharedPreferences prefs = getSharedPreferences("Work4It", MODE_PRIVATE);
        boolean blockStatus = prefs.getBoolean(packageName.toString(), false);
        if(blockStatus) {
            Log.d("AppBlockerService/isBlocked", "Successfully blocked: " + packageName);
        }
        return blockStatus;
    }
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Checks whether a new app is opened
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {

            CharSequence packageName = event.getPackageName();
            if (packageName == null) { return; }

            // Checks whether the current app open is one of the blocked apps
            if(isBlocked(packageName)) {
                // Stops the CountDownTimer and waits till conditional to reactivate
                if(countDownTimer != null) {countDownTimer.cancel();}

                // Checks how much time the user has on the blockedApp
                // If less than or equal to zero then transfer the user to the BlockOverlay Screen
                if(timeManager.getSecondsRemaining() <= 0) {
                    Intent intent = new Intent(this, BlockOverlayActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    this.startActivity(intent);
                    Log.d("AppBlockerServer:BlockOverlayActivity", "Transferred user to BlockOverlayActivity");
                }
                else {
                    // countDownTimer is in Milliseconds
                    countDownTimer = new CountDownTimer(timeManager.getSecondsRemaining() * 1000L, 1000) {
                        // Just counts down
                        @Override
                        public void onTick(long millisUntilFinished) {
                            Log.d("AppBlockerService:onTick:Before", "Remaining Time: "+ timeManager.getSecondsRemaining());
                            timeManager.subtractSeconds(1);
                            Log.d("AppBlockerService:onTick:After", "Remaining Time: " + timeManager.getSecondsRemaining());
                        }

                        // Activates if the user runs out of time while still on the blocked app
                        @Override
                        public void onFinish() {
                            Intent intent = new Intent(AppBlockerService.this, BlockOverlayActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            Log.d("AppBlockerService:onFinish", "Transferring back to BlockOverlay");
                            startActivity(intent);
                        }

                    }.start();
                }
            }
            else {
                if(countDownTimer != null) {countDownTimer.cancel();}
            }
        }
    }
    @Override
    public void onInterrupt() {
        Log.d("AppBlockerService", "Service Interrupted");
    }
}
