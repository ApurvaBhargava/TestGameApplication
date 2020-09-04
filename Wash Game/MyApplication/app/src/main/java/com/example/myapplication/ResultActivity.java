package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

public class ResultActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        Intent intentPrev = getIntent();
        int totalScore = intentPrev.getIntExtra("totalScore", -1);
        TextView totalScoreText = findViewById(R.id.ResultTotalScore);
        totalScoreText.setText("Total Score in Game: " + totalScore);
    }
}
