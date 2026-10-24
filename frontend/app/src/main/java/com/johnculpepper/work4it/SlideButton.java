package com.johnculpepper.work4it;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class SlideButton extends FrameLayout {

    private boolean isOn = false;
    private View thumb;
    private View track;
    private Context context;
    private String sliderId;
    private Boolean thumbSized = false;

    public SlideButton(@NonNull Context context) {
        super(context);
        this.context = context;
        init(context);
    }

    // Required so LayoutInflater can create this view from XML
    public SlideButton(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        init(context);
    }

    private void init(Context context) {
        // Track background
        track = new View(context);
        track.setBackgroundResource(R.drawable.btn_slider_track);
        FrameLayout.LayoutParams trackParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        );
        addView(track, trackParams);

        // Thumb
        thumb = new View(context);
        thumb.setBackgroundResource(R.drawable.btn_slider_thumb);
        // Start with 0 size - will be set after layout
        FrameLayout.LayoutParams thumbParams = new FrameLayout.LayoutParams(0, 0);
        thumbParams.leftMargin = 4;
        thumbParams.topMargin = 4;
        addView(thumb, thumbParams);
    }
    public void setupClickListener(String sliderId) {
        this.sliderId = sliderId;

        SharedPreferences prefs = context.getSharedPreferences("Work4It", Context.MODE_PRIVATE);
        isOn = prefs.getBoolean(sliderId, false);

        // Wait for view to be fully drawn
        post(() -> {
            // Resizes thumb
            if(!thumbSized) {
                int thumbSize = getHeight() - 8;
                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(thumbSize, thumbSize);
                params.leftMargin = 4;
                params.topMargin = 4;
                thumb.setLayoutParams(params);
                thumbSized = true;
            }

            // Reposition the thumb
            post(() -> {
                int trackWidth = getWidth();
                int thumbWidth = thumb.getWidth();
                int targetX = isOn ? trackWidth - thumbWidth - 4 : 4;
                thumb.setTranslationX(targetX);
                track.setBackgroundResource(isOn ?
                        R.drawable.btn_slider_track_on :
                        R.drawable.btn_slider_track);
            });
        });
        this.setOnClickListener(v -> toggle());
    }
    public void toggle() {
        isOn = !isOn;
        context.getSharedPreferences("Work4It", Context.MODE_PRIVATE)
                .edit()
                .putBoolean(sliderId, isOn)
                .apply();

        // Get track width to calculate thumb position
        int trackWidth = getWidth();
        int thumbWidth = thumb.getWidth();
        int targetX = isOn ? trackWidth - thumbWidth - 4 : 4;

        // Animate thumb sliding
        thumb.animate()
                .translationX(targetX)
                .setDuration(200)
                .start();

        // Change track color
        if(isOn){
            track.setBackgroundResource(R.drawable.btn_slider_track_on);
            Log.d("SlideButton/AppBlocked","User Blocked: " + sliderId);
        }
        else{
            track.setBackgroundResource(R.drawable.btn_slider_track);
            Log.d("SlideButton/AppUnblocked","User Unblocked: " + sliderId);
        }

    }
    public boolean isOn() {
        return isOn;
    }

}