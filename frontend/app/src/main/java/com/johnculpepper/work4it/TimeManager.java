package com.johnculpepper.work4it;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;


public class TimeManager {
    // These are constants - the "name" of the storage and key
    private static final String PREFS_NAME = "Work4It";
    private static final String KEY_SECONDS = "seconds_remaining";
    private static final String KEY_MINUTES_EARNED = "minutes_earned";
    private static final int DAILY_RESET = 0;
    private static final int DAILY_LIMIT = 2*60; // 2 hours in minutes
    private final SharedPreferences prefs;
    private final Context context;

    private int minutesEarned = 0;

    public TimeManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.context = context;
        this.minutesEarned = prefs.getInt(KEY_MINUTES_EARNED, 0);
    }
    // Get how many minutes the user has left today
    public int getSecondsRemaining() {
        return prefs.getInt(KEY_SECONDS, 0);
    }

    public int[] getTimeRemaining() {
        int totalMinutes = getSecondsRemaining() / 60;
        int hours = totalMinutes / 60;
        int minutes = totalMinutes % 60;
        int seconds = getSecondsRemaining() % 60;

        return new int[]{seconds, minutes, hours};
    }
    // Add minutes when user exercises
    public void addMinutes(int minutes) {
        int seconds = getSecondsRemaining();
        SharedPreferences.Editor editor = prefs.edit();

        if(minutesEarned < DAILY_LIMIT) {
            // Constraint to stop users from earning more than DAILY_LIMIT
            int newMinutes = Math.min(minutes, DAILY_LIMIT - minutesEarned);
            editor.putInt(KEY_SECONDS, seconds + (newMinutes * 60));
            editor.putInt(KEY_MINUTES_EARNED, minutesEarned+ newMinutes);
            editor.apply();
            minutesEarned += newMinutes;
        }
        // Daily Limit IS reached
        else {
            Intent intent = new Intent(context, CameraActivity.class);
            intent.putExtra("reached_limit", true);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // needed since context here likely isn't an Activity in every call path
            context.startActivity(intent);
            Log.d("TimeManager/DailyLimitReached", "User has reached daily limit of screen time: " + DAILY_LIMIT / 60);
        }
    }

    // Subtract seconds when user uses a blocked app, never below zero
    public void subtractSeconds(int sec) {
        int seconds = getSecondsRemaining();
        int newSeconds = Math.max(0, seconds - sec);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_SECONDS, newSeconds);
        editor.apply();
    }

    // Reset to 0 at the start of each day
    public void resetDaily() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_SECONDS, DAILY_RESET);
        editor.putInt(KEY_MINUTES_EARNED, DAILY_RESET);
        editor.apply();
    }
}
