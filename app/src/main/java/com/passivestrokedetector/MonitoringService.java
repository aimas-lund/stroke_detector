package com.passivestrokedetector;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import weka.core.Instance;


@SuppressLint("Registered")
public class MonitoringService extends ForegroundService {

    protected static final int CAMERA_CALIBRATION_DELAY = 500;  // calibration delay to give proper picture brightness
    protected int IMAGE_CAPTURE_PAUSE = 5000;                   // delay after each successful picture extracted
    protected static final String TAG = "Camera2 Service";
    protected static final int CAMERA_CHOICE = CameraCharacteristics.LENS_FACING_FRONT;
    protected static long cameraCaptureStartTime;
    protected CameraDevice cameraDevice;
    protected CameraCaptureSession captureSession;
    protected ImageReader imageReader;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;
    private StrokeClassifier classifier = new StrokeClassifier();

    private String cameraID;
    private static final int ORIENTATION_0 = 0;
    private static final int ORIENTATION_90 = 3;
    private static final int ORIENTATION_270 = 1;

    private ContourFeatureExtractor extractor;
    private FirebaseVisionFaceDetectorOptions options =
            new FirebaseVisionFaceDetectorOptions.Builder()
                    .setClassificationMode(FirebaseVisionFaceDetectorOptions.ACCURATE)
                    .setContourMode(FirebaseVisionFaceDetectorOptions.ALL_CONTOURS)
                    .setLandmarkMode(FirebaseVisionFaceDetectorOptions.NO_LANDMARKS)
                    .setClassificationMode(FirebaseVisionFaceDetectorOptions.NO_CLASSIFICATIONS)
                    .setMinFaceSize(0.15f)
                    .enableTracking()
                    .build();

    protected CameraDevice.StateCallback cameraStateCallback = new CameraDevice.StateCallback() {

        /*
        When the StateCallback is opened, the cameraDevice is referred to the global variable.
        Given that the imageReader is not always quite fully instantiated when this method is called,
        we make the tread sleep for a little while.
        Then, the cameraDevice should start extracting images.
         */
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            Log.d(TAG, "CameraDevice.StateCallback onOpened");
            cameraDevice = camera;
            try {                                   // Wait for the image reader to be instantiated
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            actOnReadyCameraDevice();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            Log.w(TAG, "CameraDevice.StateCallback onDisconnected");
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            Log.e(TAG, "CameraDevice.StateCallback onError " + error);
        }
    };

    protected CameraCaptureSession.StateCallback mStateCallback = new CameraCaptureSession.StateCallback() {

        @Override
        public void onReady(CameraCaptureSession session) {
            MonitoringService.this.captureSession = session;
            try {
                session.setRepeatingRequest(createCaptureRequest(), null, mBackgroundHandler);
                cameraCaptureStartTime = System.currentTimeMillis();
            } catch (CameraAccessException e) {
                Log.e(TAG, Objects.requireNonNull(e.getMessage()));
            }
        }

        @Override
        public void onConfigured(CameraCaptureSession session) {

        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
        }
    };

    protected ImageReader.OnImageAvailableListener onImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        /*
        This function is called when an image is available from the imageAvailableListener.
        The image will be converted into a bitmap and fed into the contour algorithm.
        For each successful picture, the thread will be put to sleep, specified by IMAGE_CAPTURE_PAUSE.
         */
        @Override
        public void onImageAvailable(ImageReader reader) {
            Log.d(TAG, "onImageAvailable");
            Image img = reader.acquireLatestImage();
            if (img != null) {
                if (System.currentTimeMillis() > cameraCaptureStartTime + CAMERA_CALIBRATION_DELAY) {
                    Log.d(TAG, "Image received");
                    Bitmap bitmap = imageToBitmap(img);             // Transform image object into bitmap

                    float degrees;//rotation degree
                    Display display = ((WindowManager)
                            getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
                    int screenOrientation = display.getRotation();

                    switch (screenOrientation) {
                        default:
                        case ORIENTATION_0:
                            degrees = 270;
                            break;
                        case ORIENTATION_90: // Landscape right
                            degrees = 180;
                            break;
                        case ORIENTATION_270: // Landscape left
                            degrees = 360;
                            break;
                    }

                    Matrix matrix = new Matrix();
                    matrix.setRotate(degrees);
                    bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

//                        MediaStore.Images.Media.insertImage(
//                                getContentResolver(),
//                                bitmap,
//                                "Test",
//                                "This is a test"
//                        );
                    FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(getResizedBitmap(bitmap, 640, 480));

                    //TODO: do some analysis on the image
                    FirebaseVisionFaceDetector detector = FirebaseVision.getInstance().getVisionFaceDetector(options);
                    Task<List<FirebaseVisionFace>> result = detector.detectInImage(image)
                            .addOnSuccessListener(
                                    faces -> {
                                        for (FirebaseVisionFace face : faces) {
                                            extractor.setFace(face);
                                            List<Double> list = extractor.extractAll();

                                            Instance instance = classifier.createInstance(classifier.getAllFeaturesFlattened(), list, StateOfFace.NORMAL);
                                            try {
                                                String output = classifier.predict(instance);
                                                if (output.equals("Drooping")) {
                                                    sendNotification("IMMEDIATE ACTION REQUIRED", "Stroke has possibly been detected");
                                                }
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    })
                            .addOnFailureListener(
                                    e -> {
                                        // Task failed with an exception
                                        Log.e("Error", Objects.requireNonNull(e.getMessage()));
                                    });
                    try {                                           // Wait to take another picture
                        Thread.sleep(IMAGE_CAPTURE_PAUSE);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                img.close();
            }
        }
    };

    /*
    Camera setup:
    Detects a front-facing camera on the device and opens it.
    An ImageReader is instantiated to create images of 480x640 resolution.
     */
    public void prepareCamera() {
        CameraManager manager = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            cameraID = getCamera(manager);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "No Camera Permission");
                return;
            }
            manager.openCamera(cameraID, cameraStateCallback, mBackgroundHandler);
            imageReader = ImageReader.newInstance(480, 640, ImageFormat.JPEG, 2);
            imageReader.setOnImageAvailableListener(onImageAvailableListener, mBackgroundHandler);
            Log.d(TAG, "imageReader created");
        } catch (CameraAccessException e) {
            Log.e(TAG, Objects.requireNonNull(e.getMessage()));
        }
    }

    public String getCamera(CameraManager manager) {
        try {
            for (String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
                int lensLocation = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (lensLocation == CAMERA_CHOICE) {
                    return cameraId;
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    protected CaptureRequest createCaptureRequest() {
        try {
            CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            builder.addTarget(imageReader.getSurface());
            return builder.build();
        } catch (CameraAccessException e) {
            Log.e(TAG, Objects.requireNonNull(e.getMessage()));
            return null;
        }
    }

    /*
    When the CameraDevice class is ready, a CaptureSession should be started
     */
    public void actOnReadyCameraDevice() {
        try {
            imageReader.getSurface();
            cameraDevice.createCaptureSession(Arrays.asList(imageReader.getSurface()), mStateCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, Objects.requireNonNull(e.getMessage()));
        }
    }

    /*
    When the Service is in start, the camera should be prepared.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand flags " + flags + " startId " + startId);
        prepareCamera();
        return super.onStartCommand(intent, flags, startId);
    }

    /*
    When the service is created, it should immediately create a separate background thread to handle the
    image capturing and for image analysis.
     */
    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate service");
        startBackgroundThread();
        try {
            classifier.load("classifierModel.arff");
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(classifier.getTAG(), "Failed to load classifier model");
        }
        super.onCreate();
    }

    /*
    When the service has ended, it should terminate the background thread.
     */
    @Override
    public void onDestroy() {
        try {
            captureSession.abortCaptures();
            stopBackgroundThread();
        } catch (CameraAccessException e) {
            Log.e(TAG, Objects.requireNonNull(e.getMessage()));
        }
        captureSession.close();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /*
    =====================================
    AUXILIARY FUNCTIONS
    =====================================
     */

    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("camera_background_thread");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private Bitmap imageToBitmap(Image img) {
        ByteBuffer buffer = img.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);

        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null);
    }

    public Activity getActivity(Context context) {
        if (context == null) return null;
        if (context instanceof Activity) return (Activity) context;
        if (context instanceof ContextWrapper)
            return getActivity(((ContextWrapper) context).getBaseContext());
        return null;
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

    private void sendNotification(String subject, String body) {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notify = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(subject)
                .setContentText(body)
                .setSmallIcon(R.mipmap.ic_launcher)
                .build();
        notify.flags |= Notification.FLAG_AUTO_CANCEL;
        manager.notify(0, notify);

        // Get instance of Vibrator from current Context
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        // Vibrate for 300 milliseconds
        v.vibrate(1000);
    }
}
