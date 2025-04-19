package com.opensafe.utils;

import android.app.Activity;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;

public class ImageUnlocker {

    private static final String TAG = "ImageUnlocker";
    private final Activity activity;
    private Interpreter tflite;

    private static final int INPUT_SIZE = 300; // expected input size for model
    private static final float CONFIDENCE_THRESHOLD = 0.5f; // confidence threshold

    public ImageUnlocker(Activity activity) {
        this.activity = activity;
        loadModel();
    }

    private void loadModel() {
        try {
            FileInputStream fis = new FileInputStream(activity.getAssets().openFd("detect.tflite").getFileDescriptor());
            FileChannel fileChannel = fis.getChannel();
            MappedByteBuffer modelBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY,
                    activity.getAssets().openFd("detect.tflite").getStartOffset(),
                    activity.getAssets().openFd("detect.tflite").getDeclaredLength());
            tflite = new Interpreter(modelBuffer);
            Log.d(TAG, "TFLite model loaded successfully");
        } catch (IOException e) {
            Log.e(TAG, "Error loading TFLite model", e);
        }
    }

    public void analyzeImage(Bitmap bitmap) {
        if (tflite == null) {
            Toast.makeText(activity, "המודל לא נטען", Toast.LENGTH_SHORT).show();
            return;
        }

        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true);
        ByteBuffer inputBuffer = convertBitmapToByteBuffer(resizedBitmap);

        float[][][] outputLocations = new float[1][10][4];
        float[][] outputClasses = new float[1][10];
        float[][] outputScores = new float[1][10];
        float[] numDetections = new float[1];

        Object[] inputs = {inputBuffer};
        Map<Integer, Object> outputs = new HashMap<>();
        outputs.put(0, outputLocations);
        outputs.put(1, outputClasses);
        outputs.put(2, outputScores);
        outputs.put(3, numDetections);

        tflite.runForMultipleInputsOutputs(inputs, outputs);

        boolean foundCar = false;
        int detectionsCount = (int) numDetections[0];

        Log.d(TAG, "Detections found: " + detectionsCount);

        for (int i = 0; i < detectionsCount; i++) {
            int detectedClass = (int) outputClasses[0][i];
            float score = outputScores[0][i];
            Log.d(TAG, "Detected class: " + detectedClass + ", score: " + score);

            if (score > CONFIDENCE_THRESHOLD && (detectedClass == 2 || detectedClass == 5 || detectedClass == 7)) {
                foundCar = true;
                break;
            }
        }

        if (foundCar) {
            Toast.makeText(activity, "רכב זוהה בהצלחה! 🔓", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(activity, "לא זוהה רכב בתמונה", Toast.LENGTH_SHORT).show();
        }
    }

    public void analyzeImage(Uri uri) {
        try {
            Bitmap bitmap = android.provider.MediaStore.Images.Media.getBitmap(activity.getContentResolver(), uri);
            analyzeImage(bitmap);
        } catch (IOException e) {
            Log.e(TAG, "Failed to load image from URI", e);
            Toast.makeText(activity, "שגיאה בטעינת התמונה", Toast.LENGTH_SHORT).show();
        }
    }

    private ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1 * INPUT_SIZE * INPUT_SIZE * 1 * 4);
        byteBuffer.order(ByteOrder.nativeOrder());

        int[] intValues = new int[INPUT_SIZE * INPUT_SIZE];
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        int pixel = 0;
        for (int i = 0; i < INPUT_SIZE; ++i) {
            for (int j = 0; j < INPUT_SIZE; ++j) {
                final int val = intValues[pixel++];
                float r = ((val >> 16) & 0xFF);
                float g = ((val >> 8) & 0xFF);
                float b = (val & 0xFF);
                float gray = (0.299f * r + 0.587f * g + 0.114f * b) / 255.f;
                byteBuffer.putFloat(gray);
            }
        }
        return byteBuffer;
    }
}