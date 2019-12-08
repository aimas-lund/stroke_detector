package com.passivestrokedetector;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;

import java.io.File;
import java.util.List;
import java.util.Objects;

import weka.core.Instance;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static String TAG = "MainActivity";
    private static final int REQUEST_EXTERNAL_STORAGE_PERMISSION = 1;
    private static final int REQUEST_CAMERA_PERMISSION = 2;
    private StrokeClassifier classifier;
    private ContourFeatureExtractor extractor;
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

        classifier = new StrokeClassifier();
        extractor = new ContourFeatureExtractor();

        // Initiate interactive features on frontend
        Button startBtn = findViewById(R.id.buttonStartService);
        Button stopBtn = findViewById(R.id.buttonStopService);
        Button trainModel = findViewById(R.id.buttonTrainModel);
        Button loadModel = findViewById(R.id.buttonLoadModel);
        Button deleteModel = findViewById(R.id.buttonDeleteModel);
        imageView = findViewById(R.id.imageView);

        startBtn.setOnClickListener(this);
        stopBtn.setOnClickListener(this);
        trainModel.setOnClickListener(this);
        loadModel.setOnClickListener(this);

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
            case R.id.buttonTrainModel: {
                try {
                    Toast.makeText(this, "Model trained successfully", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Model trained successfully");
                    loopPhotosThroughDirs(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString() + "/Camera/");
                } catch (Exception e) {
                    Toast.makeText(this, "Model could not be trained", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Model could not be trained");
                }
                break;
            }
            case R.id.buttonLoadModel: {
                try {
                    classifier.load("classifierModel.arff");
                    Toast.makeText(this, "Model loaded successfully", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Model loaded successfully");
                } catch (Exception e) {
                    Toast.makeText(this, "Model could not be found", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Model could not be found");
                }
                break;
            }
            case R.id.buttonDeleteModel: {
                classifier.delete("classifierModel.arff");
                Toast.makeText(this, "Model removed", Toast.LENGTH_SHORT).show();
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

        Boolean classifierAvailable = classifier.checkModelAvailable("classifierModel.arff");

        if (classifierAvailable) {
            Intent serviceIntent = new Intent(this, MonitoringService.class);
            serviceIntent.putExtra("inputExtra", "Foreground Stroke Detector in Android");

            ContextCompat.startForegroundService(this, serviceIntent);
        } else {
            Toast.makeText(this, "No trained model available", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "No trained model available");
        }

    }

    private void stopMonitoringService() {
        Intent serviceIntent = new Intent(this,  MonitoringService.class);
        stopService(serviceIntent);
    }

    public void loopPhotosThroughDirs(String dirsPath) throws Exception {
        try {
            File folder = new File(dirsPath);
            folder.mkdirs();
            File[] allFiles = folder.listFiles((dir, name) -> (
                    name.endsWith(".jpg") ||
                    name.endsWith(".jpeg") ||
                    name.endsWith(".png")));
            if (allFiles != null) {
                for (File file : allFiles) {
//                    Uri uri = Uri.fromFile(file);
//                    FirebaseVisionImage image = FirebaseVisionImage.fromFilePath(this, uri);
                    Bitmap bitmap = BitmapFactory.decodeFile(file.getPath());
                    FirebaseVisionImage image = FirebaseVisionImage.
                            fromBitmap(getResizedBitmap(bitmap, 640, 480));
                    buildClassifier(image);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            classifier.train();
            classifier.save("classifierModel.arff");
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(classifier.getTAG(), "Unable to train model");
        }

    }

    public Bitmap getResizedBitmap(Bitmap bm, int newHeight, int newWidth) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;

        // create a matrix for the manipulation
        Matrix matrix = new Matrix();

        // resize the bit map
        matrix.postScale(scaleWidth, scaleHeight);

        // recreate the new Bitmap
        return Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false);
    }

    private void buildClassifier(FirebaseVisionImage image) {
        FirebaseVisionFaceDetectorOptions options =
                new FirebaseVisionFaceDetectorOptions.Builder()
                        .setClassificationMode(FirebaseVisionFaceDetectorOptions.ACCURATE)
                        .setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
                        .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
                        .setMinFaceSize(0.15f)
                        .enableTracking()
                        .build();

        FirebaseVisionFaceDetector detector = FirebaseVision.getInstance().getVisionFaceDetector(options);
        Task<List<FirebaseVisionFace>> result =
                detector.detectInImage(image)
                        .addOnSuccessListener(
                                faces -> {
                                    for (FirebaseVisionFace face : faces) {
                                        extractor.setFace(face);
                                        List<Double> list = extractor.extractAll();

                                        /*
                                        TODO: Need to find a way to label images for training
                                        -- Perhaps make separate folders for each class
                                         */

                                        Instance instance = classifier.createInstance(
                                                classifier.getAllFeaturesFlattened(),
                                                list,
                                                StateOfFace.NORMAL
                                        );
                                        classifier.addToInstances(instance);
                                    }
                                })
                        .addOnFailureListener(
                                e -> {
                                    // Task failed with an exception
                                    Log.e("Error", Objects.requireNonNull(e.getMessage()));
                                });
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


