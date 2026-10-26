package com.johnculpepper.work4it;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;

import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mediapipe.framework.image.BitmapImageBuilder;
import com.google.mediapipe.framework.image.MPImage;
import com.google.mediapipe.tasks.core.BaseOptions;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker;

// Used for conversion of Image to ByteMap
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class CameraActivity extends AppCompatActivity {
    private int repCount;
    private String exerciseType;
    private boolean dailyLimitReached;
    private TimeManager timeManager;

    // Regarding camera variables
    private boolean isRecording = false;
    private final int REQUEST_CODE_CAMERA = 100;
    private ProcessCameraProvider cameraProvider;
    private CameraSelector cameraSelector;
    private ExecutorService executor;
    private String pythonServerUrl;
    private OverlayView overlayView;
    private PoseLandmarker poseLandmarker;

    private ImageAnalysis poseAnalysis;

    // Default Frames sent to Python Server
    long frameIntervalMs = 1000;
    long lastFrameTime = 0;

    private TextView repCountDisplay;
    private Preview preview;
    // Condenses images to Bytes for Mediapipe CV to use
    private Bitmap imageProxyToBitmap(ImageProxy image) {
        byte[] jpegBytes = convertImageProxyToByteArray(image);
        return android.graphics.BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.length);
    }
    protected void startCamera() {
        PreviewView previewView = findViewById(R.id.previewView);
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());
                cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;

                // Continuous pose detection - always running
                poseAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                poseAnalysis.setAnalyzer(executor, image -> {
                    int frameWidth = image.getWidth();
                    int frameHeight = image.getHeight();
                    Log.d("DeviceDimensions", "Image Dimensions: " + frameWidth + " by " + frameHeight);

                    // Passes dimensions of device to OverlayView class
                    runOnUiThread(()-> {
                        if (overlayView != null) {
                            overlayView.setImageDimensions(frameWidth, frameHeight);
                        }
                    });

                    Bitmap bitmap = imageProxyToBitmap(image);
                    MPImage mpImage = new BitmapImageBuilder(bitmap).build();
                    poseLandmarker.detectAsync(mpImage, image.getImageInfo().getTimestamp());
                    image.close();
                });

                cameraProvider.bindToLifecycle(this, cameraSelector, preview, poseAnalysis);
            } catch(Exception e) {
                Log.d("CameraActivity: startCamera", "startCamera method crashed due to " + e);
            }
        }, ContextCompat.getMainExecutor(this));
    }
    protected void requestRuntimePermissions() {
        // Checks whether Permissions where granted
        // If is then starts the Camera
        if (ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED) {
            startCamera();
            Toast.makeText(getApplicationContext(),
                    "Permission Granted", Toast.LENGTH_SHORT).show();

        // If Camera permissions is denied then the Alert Dialog will pop up
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(
                this, android.Manifest.permission.CAMERA)) {

            // The Alert Dialog that Requests for Camera Permissions
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Camera permissions is required for the system to count exercise reps towards your screen time.");
            builder.setTitle("Permissions Required")
                    .setCancelable(false)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(CameraActivity.this,
                                    new String[] {android.Manifest.permission.CAMERA},
                                    REQUEST_CODE_CAMERA);
                            dialog.dismiss();
                        }
                    });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            builder.show();
        // Initial Ask by Android for Camera Permissions
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[] {android.Manifest.permission.CAMERA},
            REQUEST_CODE_CAMERA);
        }
    }
    // Results of the Permission Request
    // Results of the Permission Request
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQUEST_CODE_CAMERA) {
            // Permission already granted
            if(grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
                Toast.makeText(getApplicationContext(),
                        "Permission Granted", Toast.LENGTH_SHORT).show();

            // Asks for Permission if Initially denied
            } else if(!ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.CAMERA)){
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("Camera permissions is required for the system to count exercise " +
                        "reps towards your screen time. Without this you will not be able to earn screen time.");
                builder.setTitle("Permissions Required")
                        .setCancelable(false)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            // Once pressing "OK" to permissions the dialog will bring the user
                            // to the Settings for them to give the app permissions
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package", getPackageName(), null);
                                intent.setData(uri);
                                startActivity(intent);
                                dialog.dismiss();
                            }
                        });
                // If the user denies to give access to Camera
                // the dialog will simply close
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                builder.show();
            }
        }
    }
    private void setFrameInterval(String exerciseType) {
        if(exerciseType == null) return;
        switch(exerciseType) {
            case "squat":
                frameIntervalMs = 100;  // 10 fps - squats
                break;
            case "pushup":
                frameIntervalMs = 200;  // 5 fps - pushups
                break;
            default:
                frameIntervalMs = 1000; // 1 fps default
        }
    }

    // Activates when user exits CameraActivity; stops camera and closes poseLandmarker
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraProvider != null) cameraProvider.unbindAll();
        if (poseLandmarker != null) poseLandmarker.close();
        executor.shutdown();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_camera);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        // Initializes the timeManager class
        this.timeManager = new TimeManager(this);

        // Access the exerciseType from the ExerciseActivity file
        this.exerciseType = getIntent().getStringExtra("exercise_type");

        executor = Executors.newSingleThreadExecutor();

        overlayView = findViewById(R.id.overlayView);

        // Setup MediaPipe PoseLandmarker
        try {
            BaseOptions baseOptions = BaseOptions.builder()
                    .setModelAssetPath("com/johnculpepper/Work4It/pose_landmarker_full.task")
                    .build();

            PoseLandmarker.PoseLandmarkerOptions options =
                    PoseLandmarker.PoseLandmarkerOptions.builder()
                            .setBaseOptions(baseOptions)
                            .setRunningMode(RunningMode.LIVE_STREAM)
                            .setResultListener((result, image) -> {
                                runOnUiThread(() -> overlayView.setResults(result));
                            })
                            .build();

            poseLandmarker = PoseLandmarker.createFromOptions(this, options);
        } catch (Exception e) {
            Log.e("CameraActivity", "Failed to initialize PoseLandmarker (Is pose_landmarker_full.task in assets?): " + e.getMessage(), e);
            Toast.makeText(this, "Model init error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        // Assigns Python IP Address url server through configure.properties
        try {
            Properties prop = new Properties();
            prop.load(getAssets().open("com/johnculpepper/Work4It/configure.properties"));
            pythonServerUrl = prop.getProperty("server.url");
            Log.d("ServerURL", "Connecting to: " + pythonServerUrl);

        } catch(IOException e){
            Log.d("CameraActivity: pythonServerUrl", "Missing python server URL file");
            pythonServerUrl = "http://YOUR-COMPUTER-IP:8000/count";
        }
        // Runs Camera Permissions Requests
        requestRuntimePermissions();

        // To Exercise Menu Button
        Button toExerciseMenu = findViewById(R.id.toExerciseMenu);
        toExerciseMenu.setOnClickListener(v -> {
            Intent intent = new Intent(CameraActivity.this, ExerciseActivity.class);
            startActivity(intent);
        });

        // Tells user the exercise type
        if(exerciseType != null) {
            TextView selectedExercise = findViewById(R.id.selectedExercise);
            selectedExercise.setText(exerciseType.toUpperCase());
        }
        // Rep display
        repCountDisplay = findViewById(R.id.COUNT);
        repCountDisplay.setText(repCount + " Reps");

        // Depending on the exercise type sets the frame interval to be sent to Python script
        setFrameInterval(exerciseType);

        // Daily Limit Reached Text View
        TextView dailyLimitText = findViewById(R.id.dailyLimitHit);
        if (timeManager.isLimitReached()) {
            dailyLimitText.setText("Reached Daily Limit of 2 Hours!");
        }
        //Activates Record Button
        int recordBtnId = R.id.recordButton;
        ImageButton recordButton = findViewById(recordBtnId);
        recordButton.setOnClickListener(v-> {
            isRecording = !isRecording;
            if(isRecording) {
                recordButton.setBackgroundResource(R.drawable.btn_record_active);
                startRecording();
            }
            else {
                recordButton.setBackgroundResource(R.drawable.btn_record_default);
                stopRecording();
            }
        });
    }
    protected static byte[] convertImageProxyToByteArray(ImageProxy image) {
        // Get the three YUV planes
        byte[] nv21 = getBytes(image);

        // Convert NV21 byte array to Bitmap
        YuvImage yuvImage = new YuvImage(
                nv21,
                ImageFormat.NV21,
                image.getWidth(),
                image.getHeight(),
                null
        );
        // Compress to JPEG bytes
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(
                new Rect(0, 0, image.getWidth(), image.getHeight()),
                80, // quality 0-100
                stream
        );
        return stream.toByteArray();
    }
    @NonNull
    private static byte[] getBytes(ImageProxy image) {
        ImageProxy.PlaneProxy[] planes = image.getPlanes();
        int width = image.getWidth();
        int height = image.getHeight();

        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();

        byte[] nv21 = new byte[width * height * 3 / 2];
        int pos = 0;

        // Copy Y plane row by row, respecting rowStride
        int yRowStride = planes[0].getRowStride();
        for (int row = 0; row < height; row++) {
            yBuffer.position(row * yRowStride);
            yBuffer.get(nv21, pos, width);
            pos += width;
        }
        // Copy interleaved VU plane, respecting rowStride and pixelStride
        int uvRowStride = planes[1].getRowStride();
        int uvPixelStride = planes[1].getPixelStride();
        int chromaHeight = height / 2;
        int chromaWidth = width / 2;

        for (int row = 0; row < chromaHeight; row++) {
            int vRowStart = row * uvRowStride;
            int uRowStart = row * uvRowStride;
            for (int col = 0; col < chromaWidth; col++) {
                nv21[pos++] = vBuffer.get(vRowStart + col * uvPixelStride);
                nv21[pos++] = uBuffer.get(uRowStart + col * uvPixelStride);
            }
        }

        return nv21;
    }
    protected void startRecording() {
        // Update poseAnalysis to also send frames to server
        poseAnalysis.setAnalyzer(executor, image -> {
            long currentTime = System.currentTimeMillis();

            // Always run pose detection for overlay
            Bitmap bitmap = imageProxyToBitmap(image);
            MPImage mpImage = new BitmapImageBuilder(bitmap).build();
            poseLandmarker.detectAsync(mpImage, image.getImageInfo().getTimestamp());

            // Only send to server at frame interval
            if(currentTime - lastFrameTime >= frameIntervalMs) {
                lastFrameTime = currentTime;
                byte[] imageBytes = convertImageProxyToByteArray(image);

                NetworkManager networkManager = new NetworkManager();
                networkManager.sendFrame(pythonServerUrl, imageBytes, exerciseType, new NetworkManager.RepCountCallback() {
                    @Override
                    public void onResult(int repCount, String stage) {
                        Log.d("CameraActivity:onResult:RepCount", "Received: " + repCount);
                        CameraActivity.this.repCount = repCount;
                        runOnUiThread(() -> repCountDisplay.setText(repCount + " Reps"));
                        runOnUiThread(() -> {
                            repCountDisplay.setText(repCount + " Reps");
                            if (repCount >= timeManager.getMinutesLeftToEarn()) {
                                ((TextView) findViewById(R.id.dailyLimitHit))
                                        .setText("Reached Daily Limit of 2 Hours!");
                                stopRecording();
                            }
                        });
                    }
                    @Override
                    public void onError(String error) {
                        Log.d("CameraActivity:onError:Network", "Error: " + error);
                    }
                });
            }
            image.close();
        });
    }
    protected void stopRecording() {
        // Restore pose-only analyzer
        poseAnalysis.setAnalyzer(executor, image -> {
            Bitmap bitmap = imageProxyToBitmap(image);
            MPImage mpImage = new BitmapImageBuilder(bitmap).build();
            poseLandmarker.detectAsync(mpImage, image.getImageInfo().getTimestamp());
            image.close();
        });

        timeManager.addMinutes(repCount);
        if (timeManager.isLimitReached()) {
            ((TextView) findViewById(R.id.dailyLimitHit))
                    .setText("Reached Daily Limit of 2 Hours!");
        }
        repCount = 0;
        NetworkManager networkManager = new NetworkManager();
        networkManager.makeGetRequest(pythonServerUrl.replace("/count", "/reset"));
    }
}
