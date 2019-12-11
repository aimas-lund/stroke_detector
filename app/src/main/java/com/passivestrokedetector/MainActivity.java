package com.passivestrokedetector;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

import android.os.Bundle;
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import weka.core.Instance;
import weka.core.Instances;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static String TAG = "MainActivity";
    private String[] PERMISSIONS = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
    };
    private static final int REQUEST_PERMISSIONS_CODE = 1;
    private StrokeClassifier classifier;
    private ContourFeatureExtractor extractor;
    private ImageView imageView;

    // Buttons
    private Button startBtn;
    private Button stopBtn;
    private Button trainModelBtn;
    private Button loadModelBtn;
    private Button deleteModelBtn;
    private Boolean buttonsDisabled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Check for permissions
        if (!hasPermissions(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, REQUEST_PERMISSIONS_CODE);
        }

        classifier = new StrokeClassifier();
        extractor = new ContourFeatureExtractor();

        // Initiate interactive features on frontend
        startBtn = findViewById(R.id.buttonStartService);
        stopBtn = findViewById(R.id.buttonStopService);
        trainModelBtn = findViewById(R.id.buttonTrainModel);
        loadModelBtn = findViewById(R.id.buttonLoadModel);
        deleteModelBtn = findViewById(R.id.buttonDeleteModel);
        imageView = findViewById(R.id.imageView);

        startBtn.setOnClickListener(this);
        stopBtn.setOnClickListener(this);
        trainModelBtn.setOnClickListener(this);
        loadModelBtn.setOnClickListener(this);
        deleteModelBtn.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.buttonStartService: {
                toggleButtons(stopBtn);
                Toast.makeText(this, "Service has started", Toast.LENGTH_SHORT).show();
                startMonitoringService();
                break;
            }
            case R.id.buttonStopService: {
                Toast.makeText(this, "Service has ended", Toast.LENGTH_SHORT).show();
                stopMonitoringService();
                toggleButtons(stopBtn);
                break;
            }
            case R.id.buttonTrainModel: {
                toggleAllButtons();
                try {
                    doTraining(getNormalImage(), getDroopingImage());
                    Toast.makeText(this, "Model trained successfully", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Model trained successfully");
//                    loopPhotosThroughDirs(filePath);
                } catch (Exception e) {
                    Toast.makeText(this, "Model could not be trained", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Model could not be trained");
                }
                toggleAllButtons();
                break;
            }
            case R.id.buttonLoadModel: {
                toggleAllButtons();
                try {
//                    classifier.load("classifierModel.arff");
                    classifier.load(getInstances());
                    Toast.makeText(this, "Model loaded successfully", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Model loaded successfully");
                } catch (Exception e) {
                    Toast.makeText(this, "Model could not be found", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Model could not be found");
                }
                toggleAllButtons();
                break;
            }
            case R.id.buttonDeleteModel: {
                toggleAllButtons();
                classifier.delete("classifierModel.arff");
                Toast.makeText(this, "Model removed", Toast.LENGTH_SHORT).show();
                toggleAllButtons();
            }
        }
    }

    public Instances getInstances() throws IOException {
        BufferedReader bReader;
        bReader = new BufferedReader(
                new InputStreamReader(ISR(R.raw.data)));
        Instances data = new Instances(bReader);
        return data;
    }

    public InputStream ISR(int resourceId) {
        InputStream iStream = getBaseContext().getResources().openRawResource(resourceId);
        return iStream;
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
        Intent serviceIntent = new Intent(this, MonitoringService.class);
        stopService(serviceIntent);
    }

    private Bitmap getResizedBitmap(Bitmap bm, int newHeight, int newWidth) {
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

    /**
     * This is used for manual images
     *
     * @param normal
     * @param drooping
     */
    private void doTraining(List<Bitmap> normal, List<Bitmap> drooping) {
        for (Bitmap map : normal) {
            map = getResizedBitmap(map, 440, 320);
            FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(map);

            // label normal
            buildClassifier(image, false);
        }


        for (Bitmap map : drooping) {
            map = getResizedBitmap(map, 440, 320);
            FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(map);

            // label drooping
            buildClassifier(image, true);
        }

        try {
            classifier.train();
            classifier.save("classifierModel.arff");
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(classifier.getTAG(), "Unable to train model");
        }
    }

    private void buildClassifier(FirebaseVisionImage image, boolean isDrooping) {

        FirebaseVisionFaceDetectorOptions options =
                new FirebaseVisionFaceDetectorOptions.Builder()
                        .setClassificationMode(FirebaseVisionFaceDetectorOptions.ACCURATE)
                        .setContourMode(FirebaseVisionFaceDetectorOptions.ALL_CONTOURS)
                        .setLandmarkMode(FirebaseVisionFaceDetectorOptions.NO_LANDMARKS)
                        .setClassificationMode(FirebaseVisionFaceDetectorOptions.NO_CLASSIFICATIONS)
                        .setMinFaceSize(0.15f)
                        .enableTracking()
                        .build();

        FirebaseVisionFaceDetector detector = FirebaseVision.getInstance().getVisionFaceDetector(options);
        Task<List<FirebaseVisionFace>> result = detector.detectInImage(image)
                .addOnSuccessListener(
                        faces -> {
                            for (FirebaseVisionFace face : faces) {
                                extractor.setFace(face);
                                List<Double> list = extractor.extractAll();

                                if (isDrooping) {
                                    Instance instance = classifier.createInstance(
                                            classifier.getAllFeaturesFlattened(),
                                            list,
                                            StateOfFace.DROOPING
                                    );
                                    classifier.addToInstances(instance);
                                } else {
                                    Instance instance = classifier.createInstance(
                                            classifier.getAllFeaturesFlattened(),
                                            list,
                                            StateOfFace.NORMAL
                                    );
                                    classifier.addToInstances(instance);
                                }
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


    private static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    private void toggleButtons(Button active) {
        List<Button> buttonArray = Arrays.asList(startBtn, stopBtn, loadModelBtn, trainModelBtn, deleteModelBtn);

        if (buttonsDisabled) {
            for (Button b : buttonArray) {
                b.setEnabled(true);
                b.setBackgroundColor(ContextCompat.getColor(this, R.color.colorAccent));
            }
        } else {
            for (Button b : buttonArray) {
                if (b != active) {
                    b.setEnabled(false);
                    b.setBackgroundColor(ContextCompat.getColor(this, R.color.colorAccentDisabled));
                }
            }
        }
        buttonsDisabled = !buttonsDisabled;
    }

    private void toggleAllButtons() {
        List<Button> buttonArray = Arrays.asList(startBtn, stopBtn, loadModelBtn, trainModelBtn, deleteModelBtn);

        if (buttonsDisabled) {
            for (Button b : buttonArray) {
                b.setEnabled(true);
                b.setBackgroundColor(ContextCompat.getColor(this, R.color.colorAccent));
            }
        } else {
            for (Button b : buttonArray) {
                b.setEnabled(false);
                b.setBackgroundColor(ContextCompat.getColor(this, R.color.colorAccentDisabled));
            }
        }
        buttonsDisabled = !buttonsDisabled;
    }

    private List<Bitmap> getDroopingImage() {
        List<Bitmap> list = new ArrayList<>();
        list.add(BitmapFactory.decodeResource(this.getResources(), R.mipmap.d1));
        list.add(BitmapFactory.decodeResource(this.getResources(), R.mipmap.d2));
        list.add(BitmapFactory.decodeResource(this.getResources(), R.mipmap.d3));
        list.add(BitmapFactory.decodeResource(this.getResources(), R.mipmap.d4));
        list.add(BitmapFactory.decodeResource(this.getResources(), R.mipmap.d5));
        list.add(BitmapFactory.decodeResource(this.getResources(), R.mipmap.d6));
        list.add(BitmapFactory.decodeResource(this.getResources(), R.mipmap.d7));
        list.add(BitmapFactory.decodeResource(this.getResources(), R.mipmap.d8));
        list.add(BitmapFactory.decodeResource(this.getResources(), R.mipmap.d9));
        list.add(BitmapFactory.decodeResource(this.getResources(), R.mipmap.d10));
        list.add(BitmapFactory.decodeResource(this.getResources(), R.mipmap.d11));
        list.add(BitmapFactory.decodeResource(this.getResources(), R.mipmap.d12));
        list.add(BitmapFactory.decodeResource(this.getResources(), R.mipmap.d13));
        list.add(BitmapFactory.decodeResource(this.getResources(), R.mipmap.d14));
        list.add(BitmapFactory.decodeResource(this.getResources(), R.mipmap.d15));
        list.add(BitmapFactory.decodeResource(this.getResources(), R.mipmap.d16));
        list.add(BitmapFactory.decodeResource(this.getResources(), R.mipmap.d17));
        list.add(BitmapFactory.decodeResource(this.getResources(), R.mipmap.d18));
        list.add(BitmapFactory.decodeResource(this.getResources(), R.mipmap.d19));
        list.add(BitmapFactory.decodeResource(this.getResources(), R.mipmap.d20));
        list.add(BitmapFactory.decodeResource(this.getResources(), R.mipmap.d21));
        list.add(BitmapFactory.decodeResource(this.getResources(), R.mipmap.d22));
        list.add(BitmapFactory.decodeResource(this.getResources(), R.mipmap.d23));
        list.add(BitmapFactory.decodeResource(this.getResources(), R.mipmap.d24));
        list.add(BitmapFactory.decodeResource(this.getResources(), R.mipmap.d25));
        list.add(BitmapFactory.decodeResource(this.getResources(), R.mipmap.d26));
        list.add(BitmapFactory.decodeResource(this.getResources(), R.mipmap.d27));
        list.add(BitmapFactory.decodeResource(this.getResources(), R.mipmap.d28));
        list.add(BitmapFactory.decodeResource(this.getResources(), R.mipmap.d29));
        list.add(BitmapFactory.decodeResource(this.getResources(), R.mipmap.d30));
        return list;
    }

    private List<Bitmap> getNormalImage() {
        List<Bitmap> list = new ArrayList<>();
        list.add(BitmapFactory.decodeResource(this.getResources(), R.mipmap.n1));
        list.add(BitmapFactory.decodeResource(this.getResources(), R.mipmap.n2));
        list.add(BitmapFactory.decodeResource(this.getResources(), R.mipmap.n3));
        list.add(BitmapFactory.decodeResource(this.getResources(), R.mipmap.n4));
        list.add(BitmapFactory.decodeResource(this.getResources(), R.mipmap.n5));
        list.add(BitmapFactory.decodeResource(this.getResources(), R.mipmap.n6));
        list.add(BitmapFactory.decodeResource(this.getResources(), R.mipmap.n7));
        list.add(BitmapFactory.decodeResource(this.getResources(), R.mipmap.n8));
        list.add(BitmapFactory.decodeResource(this.getResources(), R.mipmap.n9));
        list.add(BitmapFactory.decodeResource(this.getResources(), R.mipmap.n10));

        return list;
    }

    public StrokeClassifier getClassifier() {
        return classifier;
    }
}


