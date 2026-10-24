package com.johnculpepper.work4it;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class BlockOverlayActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_block_overlay);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Button changed to MainActivity.java Code
        Button earnButton = findViewById(R.id.button);
        earnButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // what happens when pressed?
                Intent intent = new Intent(BlockOverlayActivity.this, ExerciseActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }
}