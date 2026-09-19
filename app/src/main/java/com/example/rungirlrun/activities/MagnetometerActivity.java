package com.example.rungirlrun.activities;

import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.rungirlrun.R;
import com.example.rungirlrun.views.SpeedometerView;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class MagnetometerActivity extends AppCompatActivity
        implements SensorEventListener {

    private TextView magneticValue;
    private TextView conditionText;
    private TextView xValue;
    private TextView yValue;
    private TextView zValue;

    private SpeedometerView speedometer;

    private SensorManager sensorManager;
    private Sensor magnetometer;

    private MediaPlayer mediaPlayer;

    private boolean warningSoundPlaying = false;
    private boolean dangerSoundPlaying = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_magnetometer);

        speedometer = findViewById(R.id.speedometer);
        magneticValue = findViewById(R.id.value);
        conditionText = findViewById(R.id.show_conditions);

        xValue = findViewById(R.id.x_cor);
        yValue = findViewById(R.id.y_cor);
        zValue = findViewById(R.id.z_cor);

        setupSpeedometer();

        sensorManager =
                (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        magnetometer =
                sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        if (magnetometer == null) {

            conditionText.setText(
                    "Magnetic sensor is not available on this device."
            );

            magneticValue.setText("-- μT");

            Toast.makeText(
                    this,
                    "This phone does not have a magnetometer sensor.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void setupSpeedometer() {

        speedometer.setLabelConverter(
                new SpeedometerView.LabelConverter() {
                    @Override
                    public String getLabelFor(
                            double progress,
                            double maxProgress
                    ) {
                        return String.valueOf(
                                (int) Math.round(progress)
                        );
                    }
                }
        );

        speedometer.setMaxSpeed(100);
        speedometer.setMajorTickStep(10);
        speedometer.setMinorTicks(0);

        speedometer.addColoredRange(
                0,
                50,
                Color.rgb(121, 86, 167)
        );

        speedometer.addColoredRange(
                50,
                70,
                Color.rgb(183, 148, 214)
        );

        speedometer.addColoredRange(
                70,
                90,
                Color.rgb(255, 170, 85)
        );

        speedometer.addColoredRange(
                90,
                100,
                Color.rgb(239, 83, 111)
        );
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (magnetometer != null) {
            sensorManager.registerListener(
                    this,
                    magnetometer,
                    SensorManager.SENSOR_DELAY_NORMAL
            );
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }

        stopSound();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if (event.sensor.getType()
                != Sensor.TYPE_MAGNETIC_FIELD) {
            return;
        }

        double x = event.values[0];
        double y = event.values[1];
        double z = event.values[2];

        double magnitude = Math.sqrt(
                x * x +
                        y * y +
                        z * z
        );

        double roundedMagnitude =
                roundValue(magnitude);

        double roundedX = roundValue(x);
        double roundedY = roundValue(y);
        double roundedZ = roundValue(z);

        magneticValue.setText(
                roundedMagnitude + " μT"
        );

        xValue.setText(String.valueOf(roundedX));
        yValue.setText(String.valueOf(roundedY));
        zValue.setText(String.valueOf(roundedZ));

        if (roundedMagnitude >= 100) {
            speedometer.setSpeed(100);
        } else if (roundedMagnitude >= 0) {
            speedometer.setSpeed(roundedMagnitude);
        }

        updateDetectionStatus(magnitude);
    }

    private double roundValue(double value) {

        return new BigDecimal(value)
                .setScale(
                        0,
                        RoundingMode.HALF_UP
                )
                .doubleValue();
    }

    private void updateDetectionStatus(double magneticField) {

        if (magneticField > 90) {

            conditionText.setText(
                    "Strong electronic signal detected"
            );

            conditionText.setTextColor(
                    Color.rgb(220, 55, 85)
            );

            playDangerSound();

        } else if (magneticField > 70) {

            conditionText.setText(
                    "Potential electronic device nearby"
            );

            conditionText.setTextColor(
                    Color.rgb(210, 130, 45)
            );

            playWarningSound();

        } else {

            conditionText.setText(
                    "No unusual electronic signal detected"
            );

            conditionText.setTextColor(
                    Color.rgb(102, 80, 128)
            );

            stopSound();
        }
    }

    private void playWarningSound() {

        if (warningSoundPlaying) {
            return;
        }

        stopSound();

        mediaPlayer =
                MediaPlayer.create(
                        this,
                        R.raw.beep
                );

        if (mediaPlayer != null) {

            warningSoundPlaying = true;

            mediaPlayer.setOnCompletionListener(mp -> {
                warningSoundPlaying = false;
                mp.release();

                if (mediaPlayer == mp) {
                    mediaPlayer = null;
                }
            });

            mediaPlayer.start();
        }
    }

    private void playDangerSound() {

        if (dangerSoundPlaying) {
            return;
        }

        stopSound();

        mediaPlayer =
                MediaPlayer.create(
                        this,
                        R.raw.beepd
                );

        if (mediaPlayer != null) {

            dangerSoundPlaying = true;

            mediaPlayer.setOnCompletionListener(mp -> {
                dangerSoundPlaying = false;
                mp.release();

                if (mediaPlayer == mp) {
                    mediaPlayer = null;
                }
            });

            mediaPlayer.start();
        }
    }

    private void stopSound() {

        warningSoundPlaying = false;
        dangerSoundPlaying = false;

        if (mediaPlayer != null) {

            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
            } catch (Exception ignored) {
            }

            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    public void onAccuracyChanged(
            Sensor sensor,
            int accuracy
    ) {
        // Nothing needed here
    }
}