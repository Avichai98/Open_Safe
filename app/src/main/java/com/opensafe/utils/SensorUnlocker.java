package com.opensafe.utils;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

public class SensorUnlocker implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor magneticFieldSensor;
    private float currentMagneticFieldMagnitude = Float.MIN_VALUE;
    private MagneticFieldChangeListener magneticFieldChangeListener;

    public interface MagneticFieldChangeListener {
        void onMagneticFieldChanged(float magnitude);
    }

    public SensorUnlocker(Context context) {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        magneticFieldSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        if (magneticFieldSensor == null) {
            Log.w("SensorUnlocker", "No magnetic field sensor found");
        }
    }

    public void setMagneticFieldChangeListener(MagneticFieldChangeListener listener) {
        this.magneticFieldChangeListener = listener;
    }

    public void startListening() {
        if (magneticFieldSensor != null) {
            sensorManager.registerListener(this, magneticFieldSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    public void stopListening() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    public float getCurrentMagneticFieldMagnitude() {
        return currentMagneticFieldMagnitude;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.d("SensorUnlocker", "Accuracy changed for sensor " + sensor.getName() + ": " + accuracy);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            float magneticFieldX = event.values[0];
            float magneticFieldY = event.values[1];
            float magneticFieldZ = event.values[2];

            currentMagneticFieldMagnitude = (float) Math.sqrt(magneticFieldX * magneticFieldX +
                    magneticFieldY * magneticFieldY +
                    magneticFieldZ * magneticFieldZ);

            Log.d("SensorUnlocker", "Size of magnetic field: " + currentMagneticFieldMagnitude + " µT");

            if (magneticFieldChangeListener != null) {
                magneticFieldChangeListener.onMagneticFieldChanged(currentMagneticFieldMagnitude);
            }
        }
    }
}