package com.passivestrokedetector;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;


public class MainActivity extends AppCompatActivity {

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

    }
}
