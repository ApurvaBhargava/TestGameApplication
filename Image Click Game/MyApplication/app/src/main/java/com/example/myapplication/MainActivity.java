package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final ImageView img = findViewById(R.id.imgView);
        img.setImageResource(R.drawable.cat);

        ViewPager viewPager = findViewById(R.id.viewPager);
        ImageAdapter adapter = new ImageAdapter(this);
        viewPager.setAdapter(adapter);

        Button gotoButton = findViewById(R.id.gotonext);
        gotoButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intentNext = new Intent(MainActivity.this, TableActivity.class);
                MainActivity.this.startActivity(intentNext);
            }
        });

        //new SyncTask(MainActivity.this).execute();
        //Intent i = new Intent(this, SyncIntentService.class);
        //i.setAction("com.example.myapplication.action.sync");
        //this.startService(i);
        //st.execute();
        Intent i = new Intent(MainActivity.this, SyncIntentService.class);
        i.setAction("com.example.myapplication.action.sync");
        MainActivity.this.startService(i);

        Timer timer = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                //Log.i("TAG", "TESTER");
                UpdateTask ut = new UpdateTask(MainActivity.this);
                ut.setContext(MainActivity.this);
                ut.execute();
            }
        };
        timer.schedule(timerTask, 0, 1000);
    }
    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
    }
}