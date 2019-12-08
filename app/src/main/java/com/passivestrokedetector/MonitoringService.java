package com.passivestrokedetector;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
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
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;

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
                    if (System.currentTimeMillis () > cameraCaptureStartTime + CAMERA_CALIBRATION_DELAY) {
                        Log.d(TAG,"Image received");
                        Bitmap bitmap = imageToBitmap(img);             // Transform image object into bitmap

                        /*
                        MediaStore.Images.Media.insertImage(
                                getContentResolver(),
                                bitmap,
                                "Test",
                                "This is a test"
                        );
                         */

                        //TODO: do some analysis on the image

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
            String cameraID = getCamera(manager);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "No Camera Permission");
                return;
            }
            manager.openCamera(cameraID, cameraStateCallback, mBackgroundHandler);
            imageReader = ImageReader.newInstance(480, 640, ImageFormat.JPEG, 2);
            imageReader.setOnImageAvailableListener(onImageAvailableListener, mBackgroundHandler);
            Log.d(TAG, "imageReader created");
        } catch (CameraAccessException e){
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
        } catch (CameraAccessException e){
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
    public void actOnReadyCameraDevice()
    {
        try {
            imageReader.getSurface();
            cameraDevice.createCaptureSession(Arrays.asList(imageReader.getSurface()), mStateCallback, mBackgroundHandler);
        } catch (CameraAccessException e){
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
        Log.d(TAG,"onCreate service");
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
        } catch (CameraAccessException e){
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

        return BitmapFactory.decodeByteArray(bytes,0,bytes.length,null);
    }
}
