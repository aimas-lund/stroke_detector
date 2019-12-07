package com.passivestrokedetector;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;

import java.util.Objects;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static String TAG = "MainActivity";
    private static final int REQUEST_EXTERNAL_STORAGE_PERMISSION = 1;
    private static final int REQUEST_CAMERA_PERMISSION = 2;

    private ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Check for permissions
        checkCameraPermission();
        checkStoreagePermission();

        FirebaseVisionFaceDetectorOptions highSpeedOptions =
                new FirebaseVisionFaceDetectorOptions.Builder()
                        .setPerformanceMode(FirebaseVisionFaceDetectorOptions.FAST)
                        .setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_CONTOURS)
                        .build();

        // Initiate interactive features on frontend
        Button startBtn = findViewById(R.id.buttonStartService);
        Button stopBtn = findViewById(R.id.buttonStopService);
        Button takePhoto = findViewById(R.id.buttonTakePhoto);
        imageView = findViewById(R.id.imageView);

        startBtn.setOnClickListener(this);
        stopBtn.setOnClickListener(this);
        takePhoto.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.buttonStartService: {
                Toast.makeText(this, "Service has started", Toast.LENGTH_SHORT).show();
                startMonitoringService();
                break;
            }
            case R.id.buttonStopService: {
                Toast.makeText(this, "Service has ended", Toast.LENGTH_SHORT).show();
                stopMonitoringService();
                break;
            }
            case R.id.buttonTakePhoto: {
                Toast.makeText(this, "Photo captured", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Took Image");
            }

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data != null) {
            Bitmap bitmap = (Bitmap) Objects.requireNonNull(data.getExtras()).get("data");
            imageView.setImageBitmap(bitmap);
        }
    }

    private void startMonitoringService() {
        Intent serviceIntent = new Intent(this, MonitoringService.class);
        serviceIntent.putExtra("inputExtra", "Foreground Stroke Detector in Android");

        ContextCompat.startForegroundService(this, serviceIntent);
    }

    public void stopMonitoringService() {
        Intent serviceIntent = new Intent(this,  MonitoringService.class);
        stopService(serviceIntent);
    }


    /*
    =====================================
    AUXILIARY FUNCTIONS
    =====================================
     */

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION);
        }
    }

    private void checkStoreagePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_EXTERNAL_STORAGE_PERMISSION);
        }
    }
}


