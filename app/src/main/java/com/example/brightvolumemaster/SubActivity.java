package com.example.brightvolumemaster;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class SubActivity extends AppCompatActivity {

    SeekBar brightnessSeekBar;

    SeekBar volumeSeekBar;

    private String appName; // to store the current app name

    private String packageName;

    byte[] appIconByteArray;

    private int brightnessValue;

    private int volumeValue;

    private TextView brightnessSettingValue;

    private TextView volumeSettingValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.subactivity_main);
        initializeViews();
        loadSelectedAppDetails();
        setupSeekBarListeners();
        loadAppSettings(packageName);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Save app-specific settings
        saveAppSpecificSettings(packageName, brightnessValue, volumeValue);
    }

    private void initializeViews() {

        ImageView brightnessSeekBarImage = findViewById(R.id.brightnessIcon);
        ImageView volumeSeekBarImage = findViewById(R.id.volumeIcon);
        brightnessSeekBar = findViewById(R.id.brightnessSeekBar);
        volumeSeekBar = findViewById(R.id.volumeSeekBar);
        brightnessSettingValue = findViewById(R.id.brightnessSettingValue);
        volumeSettingValue = findViewById(R.id.volumeSettingValue);

        // Set max progress for seek bars
        brightnessSeekBar.setMax(100);
        volumeSeekBar.setMax(100);

        // Set initial visibility of UI elements
        brightnessSeekBarImage.setVisibility(View.VISIBLE);
        volumeSeekBarImage.setVisibility(View.VISIBLE);

        // Set initial progress
        int currentBrightness = getCurrentBrightness();
        int currentVolume = getCurrentVolume();

        brightnessSeekBar.setProgress(convertBrightnessToScale(currentBrightness));
        volumeSeekBar.setProgress(convertVolumeToScale(currentVolume));

        brightnessSettingValue.setText(String.valueOf(convertBrightnessToScale(currentBrightness)));
        volumeSettingValue.setText(String.valueOf(convertVolumeToScale(currentVolume)));
    }

    // Convert byte array to Drawable
    private Drawable getDrawableFromByteArray(byte[] byteArray) {
        Bitmap bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
        return new BitmapDrawable(getResources(), bitmap);
    }

    private void loadSelectedAppDetails() {
        // Set the retrieved text and drawable image into the TextView and ImageView respectively
        // Retrieve necessary app details(app name, app icon, and package name) from the Intent
        Intent intent = getIntent();

        appName = intent.getStringExtra("SELECTED_APP_NAME");
        packageName = intent.getStringExtra("PACKAGE_NAME");
        appIconByteArray = intent.getByteArrayExtra("SELECTED_APP_ICON");

        if (appName != null && appIconByteArray != null && packageName != null) {
            TextView selectedAppName = findViewById(R.id.selectedAppName);
            selectedAppName.setText(appName);

            ImageView selectedAppIcon = findViewById(R.id.selectedAppIcon);

            // Convert the byte array back to Drawable and set it to the ImageView
            Drawable iconDrawable = getDrawableFromByteArray(appIconByteArray);
            selectedAppIcon.setImageDrawable(iconDrawable);
        }
    }

    private void loadAppSettings(String packageName) {
        SharedPreferences sharedPreferences = getSharedPreferences(packageName + "_settings", MODE_PRIVATE);

        int savedBrightness = sharedPreferences.getInt(packageName + "SAVED_BRIGHTNESS", 50);
        int savedVolume = sharedPreferences.getInt(packageName + "SAVED_VOLUME", 50);

        Log.d("Settings", "Loaded settings for " + packageName + ": brightness = " + savedBrightness + ", volume = " + savedVolume);

        // Apply the saved settings to SeekBars and TextViews
        brightnessSeekBar.setProgress(savedBrightness);
        brightnessSettingValue.setText(String.valueOf(savedBrightness));
        setBrightness(convertScaleToBrightness(savedBrightness));
        brightnessValue = savedBrightness;

        volumeSeekBar.setProgress(savedVolume);
        volumeSettingValue.setText(String.valueOf(savedVolume));
        setVolume(convertScaleToVolume(savedVolume));
        volumeValue = savedVolume;
    }

    private void setupSeekBarListeners() {
        // SeekBar change listeners
        brightnessSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int systemBrightnessValue = convertScaleToBrightness(progress);
                setBrightness(systemBrightnessValue);
                brightnessSettingValue.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                brightnessSettingValue.setText(String.valueOf(brightnessSeekBar.getProgress()));
                brightnessValue = brightnessSeekBar.getProgress();
            }
        });

        volumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int systemVolumeValue = convertScaleToVolume(progress);
                setVolume(systemVolumeValue);
                volumeSettingValue.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                volumeSettingValue.setText(String.valueOf(volumeSeekBar.getProgress()));
                volumeValue = volumeSeekBar.getProgress();
            }
        });
    }

    private void saveAppSpecificSettings(String packageName, int brightness, int volume) {
        // Use SharedPreferences to store app-specific settings
        SharedPreferences preferences = getSharedPreferences(packageName + "_settings", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        // Save brightness and volume settings for the current app
        editor.putInt(packageName + "SAVED_BRIGHTNESS", brightness);
        editor.putInt(packageName + "SAVED_VOLUME", volume);

        //Apply changes
        editor.apply();

        Log.d("Settings", "Saved settings for " + packageName + ": brightness = " + brightness + ", volume = " + volume);
    }

    private int getCurrentBrightness() {
        try {
            int currentBrightnessMode = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE);

            if (currentBrightnessMode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                return -1;
            } else {
                // If brightness is set to manual mode, get the actual brightness value
                return Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
            }
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
            return -1;
        }
    }

    private void setBrightness(int value) {
        if (value < 10) {
            value = 10;
        } else if (value > 100) {
            value = 100;
        }

        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.screenBrightness = (float) value / 100;
        getWindow().setAttributes(params);
    }

    private int convertBrightnessToScale(int brightness) {
        // Convert the brightness value from the system scale (0-255) to the desired scale (0-100)
        return (int) ((brightness / 255.0) * 100);
    }

    private int convertVolumeToScale(int volume) {
        // Convert the volume value form the system scale (0-maxVolume) to the desired scale (0-100)
        int maxVolume = getMaxVolume();
        return (int) ((volume / (float) maxVolume) * 100);
    }

    private int convertScaleToBrightness(int scaleValue) {
        // Convert the brightness value from the user-friendly scale (0-100) to the system scale (0-255)
        return (int) ((scaleValue / 100.0) * 255);
    }

    private int convertScaleToVolume(int scaleValue) {
        // Convert the volume value from the user-friendly scale (0-100) to the system scale (0-maxVolume)
        int maxVolume = getMaxVolume();
        return (int) ((scaleValue / 100.0) * maxVolume);
    }

    private int getCurrentVolume() {
        AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        return audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
    }

    private int getMaxVolume() {
        AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        return audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
    }

    private void setVolume(int value) {
        AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, value, 0);
    }
}