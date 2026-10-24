package com.johnculpepper.work4it;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;

import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult;
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;

import java.util.List;
import java.util.HashMap;


public class OverlayView extends View {

    private PoseLandmarkerResult results;
    private final Paint pointPaint;
    private final Paint linePaint;
    private int imageWidth = 1;
    private int imageHeight = 1;

    public void setImageDimensions(int width, int height) {
        this.imageWidth = width;
        this.imageHeight = height;
    }
    @NonNull
    private static HashMap<Integer, int[]> getHashMap() {
        int[] leftShoulder = {12, 13, 23};   // right elbow, hip
        int[] rightShoulder = {11, 14, 24};  // right elbow, hip
        int[] leftElbow = {11, 15};          // lef wrist, shoulder
        int[] rightElbow = {12, 16};         // right wrist, shoulder
        int[] leftWrist = {};              // left elbow
        int[] rightWrist = {};             // right elbow
        int[] leftHip = {11, 24, 25};        // right hip, knee
        int[] rightHip = {12, 26};           // knee only
        int[] leftKnee = {27};           // left hip, ankle
        int[] rightKnee = {28};          // right hip, ankle
        int[] leftAnkle = {25};            // left knee
        int[] rightAnkle = {26};           // right knee

        HashMap<Integer, int[]> connectionsID = new HashMap<>();
        // Upper body
        connectionsID.put(11, leftShoulder);
        connectionsID.put(12, rightShoulder);
        connectionsID.put(13, leftElbow);
        connectionsID.put(14, rightElbow);
        connectionsID.put(15, leftWrist);
        connectionsID.put(16, rightWrist);
        // Lower body
        connectionsID.put(23, leftHip);
        connectionsID.put(24, rightHip);
        connectionsID.put(25, leftKnee);
        connectionsID.put(26, rightKnee);
        connectionsID.put(27, leftAnkle);
        connectionsID.put(28, rightAnkle);
        return connectionsID;
    }

    private float[] getRotatedCoords(NormalizedLandmark landmark) {
        // 1. Convert normalized coordinates back to raw pixel values (landscape)
        float pixelX = landmark.x() * imageWidth;
        float pixelY = landmark.y() * imageHeight;

        // 2. Rotate 90 degrees Clockwise in pixel space
        float rotatedPixelX = pixelY;
        float rotatedPixelY = imageWidth - pixelX;

        // 3. Since the frame is rotated 90 degrees, dimensions are swapped
        float rotatedImageWidth = imageHeight;
        float rotatedImageHeight = imageWidth;

        // 4. Calculate scale factors to match the overlay canvas perfectly
        float scaleX = (float) getWidth() / rotatedImageWidth;
        float scaleY = (float) getHeight() / rotatedImageHeight;

        // 5. Map the rotated pixels to your Android screen space
        float canvasX = rotatedPixelX * scaleX;
        float canvasY = rotatedPixelY * scaleY;

        return new float[]{canvasX, canvasY};
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (results == null || results.landmarks().isEmpty()) return;
        // Connections
        HashMap<Integer, int[]> connectionsID = getHashMap();

        canvas.scale(-1f, 1f, getWidth() / 2f, getHeight() / 2f);

        List<NormalizedLandmark> landmarks = results.landmarks().get(0);

        float visibilityThreshold = 0.5f;

        // Checks landmark visibility and then draw joints
        for (int id = 0; id < landmarks.size(); id++) {
            NormalizedLandmark landmark = landmarks.get(id);
            if(connectionsID.containsKey(id)) {
                float normX = landmark.x();
                float normY = landmark.y() ;
                float visibility = landmark.visibility().orElse(1.0f);

                boolean isInsideFrame = (normX >= 0.0f && normX <= 1.0f) && (normY >= 0.0f && normY <= 1.0f);
                boolean isVisible = visibility >= visibilityThreshold;

                if(isInsideFrame && isVisible) {
                    // Rotates coordinates due to Landmark feature
                    float[] coords = getRotatedCoords(landmark);
                    canvas.drawCircle(coords[0], coords[1], 8f, pointPaint);

                }
                int[] connections = connectionsID.get(id);
                assert connections != null;
                for (int end : connections) {
                    drawLine(canvas, landmarks, id, end);
                }
            }
        }
    }

    public OverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // Paint for joints
        pointPaint = new Paint();
        pointPaint.setColor(Color.RED);
        pointPaint.setStrokeWidth(20f);
        pointPaint.setStyle(Paint.Style.FILL);

        // Paint for lines between joints
        linePaint = new Paint();
        linePaint.setColor(Color.BLUE);
        linePaint.setStrokeWidth(10f);
        linePaint.setStyle(Paint.Style.STROKE);
    }
    public void setResults(PoseLandmarkerResult results) {
        this.results = results;
        invalidate(); // triggers redraw
    }
    private void drawLine(Canvas canvas, List<NormalizedLandmark> landmarks, int start, int end) {
        NormalizedLandmark startLandmark = landmarks.get(start);
        NormalizedLandmark endLandmark = landmarks.get(end);

        // Visibility Check
        float startVis = startLandmark.visibility().orElse(0f);
        float endVis = endLandmark.visibility().orElse(0f);
        if(startVis < 0.5f || endVis < 0.5f) return;

        float[] startCoords = getRotatedCoords(startLandmark);
        float[] endCoords = getRotatedCoords(endLandmark);


        canvas.drawLine(
                startCoords[0], startCoords[1],
                endCoords[0], endCoords[1],
                linePaint
        );
    }
}