package com.passivestrokedetector;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private int REQUEST_CODE_PERMISSIONS = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FirebaseVisionFaceDetectorOptions highSpeedOptions =
                new FirebaseVisionFaceDetectorOptions.Builder()
                        .setPerformanceMode(FirebaseVisionFaceDetectorOptions.FAST)
                        .setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_CONTOURS)
                        .build();

        ImageAnalyzer imageAnalyzer = new ImageAnalyzer(this);

        Button startBtn = findViewById(R.id.buttonStartService);
        Button stopBtn = findViewById(R.id.buttonStopService);

        startBtn.setOnClickListener(this);
        stopBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.buttonStartService: {
                Toast.makeText(this, "Service has started", Toast.LENGTH_SHORT).show();
                startService();
                break;
            }
            case R.id.buttonStopService: {
                Toast.makeText(this, "Service has ended", Toast.LENGTH_SHORT).show();
                stopService();
                break;
            }
        }
    }
    private void startService() {
        Intent serviceIntent = new Intent(this, ForegroundService.class);
        serviceIntent.putExtra("inputExtra", "Foreground Stroke Detector in Android");

        ContextCompat.startForegroundService(this, serviceIntent);
    }

    public void stopService() {
        Intent serviceIntent = new Intent(this, ForegroundService.class);
        stopService(serviceIntent);
    }
}


