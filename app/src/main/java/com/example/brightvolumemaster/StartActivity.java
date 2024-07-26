package com.example.brightvolumemaster;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class StartActivity extends AppCompatActivity {
    private boolean isAccessibilityPermissionGranted;
    private boolean isSystemSettingsPermissionGranted;

    private static final String PREFS_NAME = "StartActivitySettings";

    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;

    private ImageView accessibilityTick;

    private ImageView systemSettingsTick;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        // Initialize UI elements
        TextView explanationText = findViewById(R.id.explanationText);
        Button accessibilityButton = findViewById(R.id.accessibilityButton);
        accessibilityTick = findViewById(R.id.accessibilityTick);
        Button systemSettingsButton = findViewById(R.id.systemSettingsButton);
        systemSettingsTick = findViewById(R.id.systemSettingsTick);

        // Set initial visibility of UI elements
        explanationText.setVisibility(View.VISIBLE);
        accessibilityButton.setVisibility(View.VISIBLE);
        systemSettingsButton.setVisibility(View.VISIBLE);

        //Initialize SharedPreferences and Editor
        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        editor = preferences.edit();
        editor.putBoolean("accessibilityPermissionAttempted", false);
        editor.putBoolean("systemModificationAttempted", false);
        editor.putBoolean("doNotShowAgain", false);
        editor.apply();

        // Set click listener for the accessibility button
        accessibilityButton.setOnClickListener(v -> {
            // Check if accessibility service is enabled
            isAccessibilityPermissionGranted = isAccessibilityPermissionGranted();

            boolean accessibilityPermissionAttempted = preferences.getBoolean("accessibilityPermissionAttempted", false);
            Log.d("accessibilityPermissionAttempted", "the boolean value of accessibilityPermissionAttempted : " + accessibilityPermissionAttempted);

            // Check if the user has attempted to grant accessibility permission before
            if (!accessibilityPermissionAttempted) {
                // If it's the first attempt, show the confirmation dialog
                showPermissionDialog("알림", "접근성 권한 필요 : BrightVolume Master는 사용자가 설정한 앱별 밝기와 볼륨을 앱에 적용하기 위해 접근성 권한을 사용합니다.\n\n" +
                        "이 권한은 디바이스가 현재 실행하고 있는 앱을 감지하기 위한 용도로만 사용되며, 접근성 설정 창의 '설치된 서비스' 혹은 '다운로드된 앱'에서 [BrightVolume Master] 항목을 눌러 활성화할 수 있습니다.",
                        "취소", "설정 화면으로 이동",
                        (dialog, which) -> {
                            // User clicked "취소"
                            //Dismiss the dialog without any further action
                        },
                        (dialog, which) -> {
                            // User clicked "설정 화면으로 이동"
                            editor.putBoolean("accessibilityPermissionAttempted", true);
                            editor.apply();

                            // Open accessibility settings
                            openAccessibilitySettings();
                        });
            } else if (!isAccessibilityPermissionGranted) {
                // Check if the user has clicked "다시 표시하지 않음" in the past
                boolean doNotShowAgain = preferences.getBoolean("doNotShowAgain", false);
                if (!doNotShowAgain) {
                    showPermissionDialog("알림", "[BrightVolume Master] 항목은 '설치된 서비스' 혹은 '다운로드 된 앱'에 있을 수 있습니다.",
                            "다시 표시하지 않음", "확인",
                            (dialog, which) -> {
                                // User clicked "다시 표시하지 않음"
                                // Set the flag in SharedPreferences
                                editor.putBoolean("doNotShowAgain", true);
                                editor.apply();
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
            } else {
                // Display a guidance message indicating that the accessibility service is already enabled
                showPermissionDialog("알림", "접근성 권한이 이미 활성화되어 있습니다.",
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
            }
        });

        // Set click listener for the system settings button
        systemSettingsButton.setOnClickListener(v -> {
            boolean systemModificationAttempted = preferences.getBoolean("systemModificationAttempted", false);
            Log.d("ButtonClick", "System Settings Button Clicked - systemModificationAttempted: " + systemModificationAttempted);

            // Check if the user has attempted to grant system modification permission before
            if (!systemModificationAttempted) {
                // If it's the first attempt, show the confirmation dialog
                showPermissionDialog("알림", "시스템 수정 권한 필요 : BrightVolume Master는 앱별 밝기를 조정하기 위해 시스템 수정 권한을 사용합니다.\n\n" +
                                "이 권한은 사용자가 설정한 앱별 밝기 값을 실행 중인 안드로이드 앱에 적용하기 위한 용도로만 사용되며, 다른 시스템 설정에는 영향을 미치지 않습니다.",
                        "취소", "설정 화면으로 이동",
                        (dialog, which) -> {
                            // User clicked "취소"
                            //Dismiss the dialog without any further action
                        },
                        (dialog, which) -> {
                            // User clicked "설정 화면으로 이동"
                            editor.putBoolean("systemModificationAttempted", true);
                            editor.apply();
                            openWriteSettingsPermission();
                        });
            } else {
                // Open system settings modification permission
                openWriteSettingsPermission();
            }
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
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    private final ActivityResultLauncher<Intent> writeSettingsLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            // The user granted the WRITE_SETTINGS permission
                            if (Settings.System.canWrite(this)) {
                                // Handle the permission being granted
                                Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            // The user did not grant the permission
                            // Handle it accordingly, e.g., show a message to the user
                            Toast.makeText(this, "Permission not granted", Toast.LENGTH_SHORT).show();
                        }
                    });

    // Method to open system settings for modification permission
    private void openWriteSettingsPermission() {
        // Check if the app has the WRITE_SETTINGS permission
        if (Settings.System.canWrite(this)) {
            // The app already has the permission, open system settings directly
            Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } else {
            // Request the WRITE_SETTINGS permission
            Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            writeSettingsLauncher.launch(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }
    }

    // Method to display a guidance message
    private void showPermissionDialog(String title, String message, String negativeButtonText, String positiveButtonText,
                                      DialogInterface.OnClickListener negativeClickListener,
                                      DialogInterface.OnClickListener positiveClickListener) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setNegativeButton(negativeButtonText, negativeClickListener)
                .setPositiveButton(positiveButtonText, positiveClickListener)
                .show();
    }
}
