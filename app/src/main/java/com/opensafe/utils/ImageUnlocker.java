package com.opensafe.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.widget.Toast;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.objects.DetectedObject;
import com.google.mlkit.vision.objects.ObjectDetection;
import com.google.mlkit.vision.objects.ObjectDetector;
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions;

import java.util.List;

public class ImageUnlocker {
    private final Context context;
    private final UnlockCallback callback;

    public interface UnlockCallback {
        void onUnlockDetected(String detectedObject);
    }

    public ImageUnlocker(Context context, UnlockCallback callback) {
        this.context = context;
        this.callback = callback;
    }

    public void analyzeImage(Bitmap bitmap) {
        InputImage image = InputImage.fromBitmap(bitmap, 0);

        ObjectDetectorOptions options =
                new ObjectDetectorOptions.Builder()
                        .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
                        .enableMultipleObjects()
                        .enableClassification()
                        .build();

        ObjectDetector objectDetector = ObjectDetection.getClient(options);

        objectDetector.process(image)
                .addOnSuccessListener(this::processDetectedObjects)
                .addOnFailureListener(e -> Log.e("ImageUnlocker", "Failed to process image", e));
    }

    private void processDetectedObjects(List<DetectedObject> detectedObjects) {
        if (detectedObjects.isEmpty()) {
            Log.e("ImageUnlocker", "No objects detected in the image.");
            return;
        }

        for (DetectedObject object : detectedObjects) {
            if (!object.getLabels().isEmpty()) {
                String detectedLabel = object.getLabels().get(0).getText();
                Log.d("ImageUnlocker", "Detected object: " + detectedLabel); // Debugging

                if (detectedLabel.equalsIgnoreCase("food")) {
                    if (callback != null) {
                        callback.onUnlockDetected(detectedLabel);
                    }
                    return;
                }
            }
        }
        Toast.makeText(context, "No food detected. Try a different image.", Toast.LENGTH_SHORT).show();
    }
}