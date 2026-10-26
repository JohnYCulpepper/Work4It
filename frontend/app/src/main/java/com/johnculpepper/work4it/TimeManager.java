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
    // Returns how many minutes were actually added (0 if the daily limit is already reached)
    public int addMinutes(int minutes) {
        int earned = prefs.getInt(KEY_MINUTES_EARNED, 0);
        int allowance = DAILY_LIMIT - earned;
        if (allowance <= 0 || minutes <= 0) {
            Log.d("TimeManager/DailyLimitReached", "Nothing added. Earned: " + earned);
            return 0;
        }
        int newMinutes = Math.min(minutes, allowance);
        prefs.edit()
                .putInt(KEY_SECONDS, getSecondsRemaining() + newMinutes * 60)
                .putInt(KEY_MINUTES_EARNED, earned + newMinutes)
                .apply();
        return newMinutes;
    }
    // Subtract seconds when user uses a blocked app, never below zero
    public void subtractSeconds(int sec) {
        int newSeconds = Math.max(0, getSecondsRemaining() - sec);
        prefs.edit().putInt(KEY_SECONDS, newSeconds).apply();
    }

    public boolean isLimitReached() {
        return prefs.getInt(KEY_MINUTES_EARNED, 0) >= DAILY_LIMIT;
    }
    public int getMinutesLeftToEarn() {
        return Math.max(0, DAILY_LIMIT - prefs.getInt(KEY_MINUTES_EARNED, 0));
    }
    public void resetDaily() {
        prefs.edit()
                .putInt(KEY_SECONDS, DAILY_RESET)
                .putInt(KEY_MINUTES_EARNED, DAILY_RESET)
                .apply();
    }
}
