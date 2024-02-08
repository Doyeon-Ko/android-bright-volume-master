package com.example.brightvolumemaster;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class StartActivity extends AppCompatActivity {

    private TextView explanationText;
    private Button accessibilityButton;
    private Button systemSettingsButton;
    private boolean isAccessibilityPermissionGranted;
    private boolean isSystemSettingsPermissionGranted;

    private ImageView accessibilityTick;

    private ImageView systemSettingsTick;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        // Initialize UI elements
        explanationText = findViewById(R.id.explanationText);
        accessibilityButton = findViewById(R.id.accessibilityButton);
        accessibilityTick = findViewById(R.id.accessibilityTick);
        systemSettingsButton = findViewById(R.id.systemSettingsButton);
        systemSettingsTick = findViewById(R.id.systemSettingsTick);

        // Set initial visibility of UI elements
        explanationText.setVisibility(View.VISIBLE);
        accessibilityButton.setVisibility(View.VISIBLE);
        systemSettingsButton.setVisibility(View.VISIBLE);

        // Set click listener for the accessibility button
        accessibilityButton.setOnClickListener(v -> {
            // Check if accessibility service is enabled
            isAccessibilityPermissionGranted = isAccessibilityPermissionGranted();
            if (isAccessibilityPermissionGranted) {
                // Display a guidance message indicating that the accessibility service is already enabled
                showGuidanceDialog("알림", "접근성 권한이 이미 활성화되어 있습니다.",
                        "확인", "설정 화면으로 이동",
                        (dialog, which) -> {
                            // User clicked "확인"
                            // Dismiss the dialog without any further action
                        },
                        (dialog, which) -> {
                            // User clicked "설정 화면으로 이동"
                            // Open accessibility settings
                            openAccessibilitySettings();
                        });
            } else {
                // Display a confirmation dialog with "취소" and "접근성 권한 활성화" buttons
                showConfirmationDialog();
            }
        });

        // Set click listener for the system settings button
        systemSettingsButton.setOnClickListener(v -> {
            // Open system settings for modification permission
            openWriteSettingsPermission();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Check if both permissions are granted
        isAccessibilityPermissionGranted = isAccessibilityPermissionGranted();
        isSystemSettingsPermissionGranted = isSystemSettingsPermissionGranted();

        if (isAccessibilityPermissionGranted && isSystemSettingsPermissionGranted) {
            // Transition to MainActivity
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish(); // finish the StartActivity to prevent going back
        }

        // Check if accessibility service is enabled
        if (isAccessibilityPermissionGranted) {
            accessibilityTick.setVisibility(View.VISIBLE);
        } else {
            accessibilityTick.setVisibility(View.GONE);
        }

        // Check if system settings is enabled
        if (isSystemSettingsPermissionGranted) {
            systemSettingsTick.setVisibility(View.VISIBLE);
        } else {
            systemSettingsTick.setVisibility(View.GONE);
        }

        // Check if the user has clicked "다시 표시하지 않음" in the past
        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        boolean doNotShowAgain = preferences.getBoolean("doNotShowAgain", false);

        accessibilityButton.setOnClickListener(v -> {

            // Show guidance message if the permission is not granted
            if (!isAccessibilityPermissionGranted && !doNotShowAgain) {
                showGuidanceDialog("알림", "[앱별 밝기] 항목은 '설치된 서비스' 혹은 '다운로드 된 앱'에 있을 수 있습니다.",
                        "다시 표시하지 않음", "확인",
                        (dialog, which) -> {
                            // User clicked "다시 표시하지 않음"
                            // Set the flag in SharedPreferences
                            preferences.edit().putBoolean("doNotShowAgain", true).apply();
                            // Dismiss the dialog without any further action
                        },
                        (dialog, which) -> {
                            // User clicked "확인"
                            // Open accessibility settings
                            openAccessibilitySettings();
                        });
            } else {
                openAccessibilitySettings();
            }
        });

        systemSettingsButton.setOnClickListener(v -> {
            if (!isSystemSettingsPermissionGranted) {
                openWriteSettingsPermission();
            }
        });
    }

    // Method to check if accessibility service is enabled
    private boolean isAccessibilityPermissionGranted() {
        // Define the package name and class name of your accessibility service
        String packageName = "com.example.brightvolumemaster";
        String serviceName = "com.example.brightvolumemaster.BrightnessAccessibilityService";

        String fullServiceName = packageName + "/" + serviceName;

        int accessibilityEnabled = 0;
        try {
            accessibilityEnabled = Settings.Secure.getInt(
                    getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_ENABLED);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }

        if (accessibilityEnabled == 1) {
            String enabledServices = Settings.Secure.getString(
                    getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (enabledServices != null) {
                return enabledServices.contains(fullServiceName);
            }
        }

        return false;
    }

    // Method to check if modification of system settings is allowed
    private boolean isSystemSettingsPermissionGranted() {
        return Settings.System.canWrite(this);
    }

    // Method to open accessibility settings
    private void openAccessibilitySettings() {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        startActivity(intent);
    }

    // Method to open system settings for modification permission
    private void openWriteSettingsPermission() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }

    // Method to display a guidance message
    private void showGuidanceDialog(String title, String message, String negativeButtonText, String positiveButtonText,
                                    DialogInterface.OnClickListener negativeClickListener,
                                    DialogInterface.OnClickListener positiveClickListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title)
                .setMessage(message)
                .setNegativeButton(negativeButtonText, negativeClickListener) // "확인" button with null click listener to dismiss the dialog
                .setPositiveButton(positiveButtonText, positiveClickListener)
                .show();
    }

    // Method to display a confirmation dialog
    private void showConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("알림")
                .setMessage("접근성 권한을 통해 BrightVolume Master는 사용자가 현재 실행하고 있는 애플리케이션을 감지합니다. [앱별 밝기] 항목은 디바이스의 설정 중 '설치된 서비스' 혹은 '다운로드된 앱'에서 찾을 수 있습니다.")
                .setNegativeButton("취소", null) // "취소" button with null click listener to dismiss the dialog
                .setPositiveButton("접근성 권한 활성화", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Open accessibility settings when "접근성 권한 활성화" button is clicked
                        openAccessibilitySettings();
                    }
                })
                .show();
    }
}

