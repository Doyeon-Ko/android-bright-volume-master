package com.example.brightvolumemaster;

import android.accessibilityservice.AccessibilityService;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.provider.Settings;
import android.media.AudioManager;
import android.view.accessibility.AccessibilityEvent;

import java.util.HashSet;
import java.util.Set;

public class BrightnessAccessibilityService extends AccessibilityService {
    private boolean isDefaultValuesRetrieved = false;

    // Retrieve the system's maximum brightness and volume values
    int maxBrightness = 255;
    int defaultBrightness = (int) (0.5 * maxBrightness);

    int systemBrightness;

    int systemVolume;

    AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
    int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
    int defaultVolume = (int) (0.5 * maxVolume);

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        String packageName;

        // Retrieve system's default brightness and volume values
        if (!isDefaultValuesRetrieved) {
            systemBrightness = getSystemBrightness();
            systemVolume = getSystemVolume();
            isDefaultValuesRetrieved = true;
        }

        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            // The window state has changed, indicating a change in the foreground app
            packageName = String.valueOf(event.getPackageName());

            // Retrieve uniqueAppPackageNames from SharedPreferences
            SharedPreferences preferencesUserApp = getApplicationContext().getSharedPreferences("UserApp", Context.MODE_PRIVATE);
            Set<String> uniqueAppPackageNames = preferencesUserApp.getStringSet("uniqueAppPackageNames", new HashSet<>());

            if (uniqueAppPackageNames.contains(packageName)) {
                    // Use packageName to access SharedPreferences
                    SharedPreferences preferencesSettingValue = getApplicationContext().getSharedPreferences(packageName + "_settings", Context.MODE_PRIVATE);

                    // Retrieve brightness and volume values based on the current package name
                    int brightnessValue = preferencesSettingValue.getInt(packageName + "SAVED_BRIGHTNESS", defaultBrightness);
                    int volumeValue = preferencesSettingValue.getInt(packageName + "SAVED_VOLUME", defaultVolume);

                    int systemBrightnessValue = convertScaleToBrightness(brightnessValue);
                    int systemVolumeValue = convertScaleToVolume(volumeValue);

                    // Adjust brightness and volume based on the retrieved values
                    adjustBrightness(systemBrightnessValue);
                    adjustVolume(systemVolumeValue);
                } else {
                // Apply the system's default brightness and volume values when the user is not running any Android app.
                adjustBrightness(systemBrightness);
                adjustVolume(systemVolume);
            }
        }
    }

    @Override
    public void onInterrupt() {
        // This method is called when the accessibility service is interrupted or disabled
    }

    // Method to get the default brightness value of system
    private int getSystemBrightness() {
        try {
            ContentResolver contentResolver = getContentResolver();
            return Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();

            return defaultBrightness;
        }
    }

    // Method to get the default volume value of system
    private int getSystemVolume() {
        return audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
    }

    // Method to adjust brightness based on the foreground app
    private void adjustBrightness(int brightnessValue) {
        if (Settings.System.canWrite(this)) {
            try {
                // Adjust the screen brightness using Settings.System
                Settings.System.putInt(
                        getContentResolver(),
                        Settings.System.SCREEN_BRIGHTNESS,
                        brightnessValue
                );
            } catch (SecurityException e) {
                e.printStackTrace();
                // Handle the security exception (e.g., inform the user about the error)
            }
        } else {
            // Request the WRITE_SETTINGS permission at runtime
            Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        }
    }

    private int convertScaleToBrightness(int scaleValue) {
        // Convert the brightness value from the user-friendly scale (0-100) to the system scale (0-255)
        return (int) ((scaleValue / 100.0) * 255);
    }

    private int convertScaleToVolume(int scaleValue) {
        // Convert the volume value from the user-friendly scale (0-100) to the system scale (0-maxVolume)
        return (int) ((scaleValue / 100.0) * maxVolume);
    }

    // Method to adjust (media) volume based on the foreground app
    private void adjustVolume(int volumeValue) {
        // Use AudioManager to adjust the media volume
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volumeValue, 0);
    }
}

