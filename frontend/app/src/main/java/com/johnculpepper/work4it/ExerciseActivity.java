package com.johnculpepper.work4it;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.HashMap;
import java.util.Map;

public class ExerciseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_exercise);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        // To Home button
        Button toHome = findViewById(R.id.toHome);
        toHome.setOnClickListener(v ->{
            Intent intent = new Intent(ExerciseActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });

        // exerciseType dictionary
        Map<Integer, String> exerciseButtons = new HashMap<>();
        exerciseButtons.put(R.id.exerciseSquat, "squat");
        exerciseButtons.put(R.id.exercisePushup, "pushup");
        for(Map.Entry<Integer, String> entry: exerciseButtons.entrySet()) {
            int exerciseId = entry.getKey();
            String exerciseType = entry.getValue();

            Button button = findViewById(exerciseId);
            if (button != null) {
                button.setOnClickListener(v-> {
                    Intent intent = new Intent(ExerciseActivity.this, CameraActivity.class);
                    // Used so that the CameraActivity class can get the exercise type
                    intent.putExtra("exercise_type", exerciseType);
                    startActivity(intent);
                    finish();
                });
            }
        }
    }
}