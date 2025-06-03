package tech.wdg.incomingactivitygateway;

import android.Manifest;
import android.app.ActivityManager;
import android.app.NotificationManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class SettingsActivity extends AppCompatActivity {

    private static final String TAG = "SettingsActivity";
    private TextView syslogsText;
    private TextView appVersion;
    private TextView serviceStatusText;
    private TextView aboutText;
    private Chip chipServiceStatus;

    // Permission chips
    private Chip chipSmsPermission;
    private Chip chipPhoneStatePermission;
    private Chip chipCallLogPermission;
    private Chip chipContactsPermission;
    private Chip chipPhoneNumbersPermission;
    private Chip chipNotificationsPermission;
    private Chip chipNotificationListenerPermission;
    private Chip chipWifiPermission;

    private MaterialButton btnRefreshLogs;
    private MaterialButton btnClearLogs;
    private MaterialButton btnCopyLogs;
    private MaterialButton btnOperatorSettings;
    private MaterialButton btnAppWebhooks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Handle window insets for edge-to-edge
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            v.setPadding(0, insets.getInsets(WindowInsetsCompat.Type.systemBars()).top, 0, 0);
            return insets;
        });

        setupToolbar();
        initializeViews();
        setupClickListeners();
        setupClickableText();
        loadSystemLogs();
        updateAppInfo();
        updateServiceStatus();
        updateAllPermissionStatus();
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }

    private void initializeViews() {
        syslogsText = findViewById(R.id.syslogs_text);
        appVersion = findViewById(R.id.app_version);
        serviceStatusText = findViewById(R.id.service_status_text);
        aboutText = findViewById(R.id.about_text);
        chipServiceStatus = findViewById(R.id.chip_service_status);

        // Initialize permission chips
        chipSmsPermission = findViewById(R.id.chip_sms_permission);
        chipPhoneStatePermission = findViewById(R.id.chip_phone_state_permission);
        chipCallLogPermission = findViewById(R.id.chip_call_log_permission);
        chipContactsPermission = findViewById(R.id.chip_contacts_permission);
        chipPhoneNumbersPermission = findViewById(R.id.chip_phone_numbers_permission);
        chipNotificationsPermission = findViewById(R.id.chip_notifications_permission);
        chipNotificationListenerPermission = findViewById(R.id.chip_notification_listener_permission);
        chipWifiPermission = findViewById(R.id.chip_wifi_permission);

        btnRefreshLogs = findViewById(R.id.btn_refresh_logs);
        btnClearLogs = findViewById(R.id.btn_clear_logs);
        btnCopyLogs = findViewById(R.id.btn_copy_logs);
        btnOperatorSettings = findViewById(R.id.btn_operator_settings);
        btnAppWebhooks = findViewById(R.id.btn_app_webhooks);
    }

    private void setupClickListeners() {
        btnRefreshLogs.setOnClickListener(v -> loadSystemLogs());
        btnClearLogs.setOnClickListener(v -> clearSystemLogs());
        btnCopyLogs.setOnClickListener(v -> copyLogsToClipboard());
        btnOperatorSettings.setOnClickListener(v -> openOperatorSettings());
        btnAppWebhooks.setOnClickListener(v -> openAppWebhooks());

        // Add click listeners for permission chips to open settings
        chipSmsPermission.setOnClickListener(v -> openAppSettings());
        chipPhoneStatePermission.setOnClickListener(v -> openAppSettings());
        chipCallLogPermission.setOnClickListener(v -> openAppSettings());
        chipContactsPermission.setOnClickListener(v -> openAppSettings());
        chipPhoneNumbersPermission.setOnClickListener(v -> openAppSettings());
        chipWifiPermission.setOnClickListener(v -> openAppSettings());
        chipNotificationsPermission.setOnClickListener(v -> openAppSettings());
        chipNotificationListenerPermission.setOnClickListener(v -> openNotificationListenerSettings());
    }

    private void setupClickableText() {
        String text = "Proudly Vibe-coded by @wedigital using Claude \\ Cursor \\ Chang beer";
        SpannableString spannableString = new SpannableString(text);

        // Find the @wedigital part
        int start = text.indexOf("@wedigital");
        int end = start + "@wedigital".length();

        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                openTelegramLink();
            }
        };

        spannableString.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        aboutText.setText(spannableString);
        aboutText.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void openTelegramLink() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/wedigital"));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Unable to open link", Toast.LENGTH_SHORT).show();
        }
    }

    private void openAppSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", getPackageName(), null);
            intent.setData(uri);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Unable to open app settings", Toast.LENGTH_SHORT).show();
        }
    }

    private void openNotificationListenerSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Unable to open notification settings", Toast.LENGTH_SHORT).show();
        }
    }

    private void copyLogsToClipboard() {
        String logs = syslogsText.getText().toString();

        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("System Logs", logs);
        clipboard.setPrimaryClip(clip);

        Toast.makeText(this, "Logs copied to clipboard", Toast.LENGTH_SHORT).show();
    }

    private void loadSystemLogs() {
        syslogsText.setText("Loading logs...");

        Thread logThread = new Thread(() -> {
            String logs = "";
            try {
                String[] command = new String[] {
                        "logcat", "-d", "*:E", "-m", "1000",
                        "|", "grep", "tech.wdg.incomingactivitygateway" };
                Process process = Runtime.getRuntime().exec(command);

                BufferedReader bufferedReader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()));

                StringBuilder logBuilder = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    logBuilder.append(line).append("\n");
                }
                logs = logBuilder.toString();

                if (logs.isEmpty()) {
                    logs = "No error logs found for this application.";
                }
            } catch (IOException ex) {
                logs = "Failed to retrieve logs: " + ex.getMessage();
                Log.e("SettingsActivity", "getLog failed", ex);
            }

            final String finalLogs = logs;
            runOnUiThread(() -> syslogsText.setText(finalLogs));
        });
        logThread.start();
    }

    private void clearSystemLogs() {
        Thread clearThread = new Thread(() -> {
            try {
                String[] command = new String[] { "logcat", "-c" };
                Runtime.getRuntime().exec(command);

                runOnUiThread(() -> {
                    syslogsText.setText("Logs cleared successfully.");
                    // Reload logs after a short delay
                    syslogsText.postDelayed(this::loadSystemLogs, 1000);
                });
            } catch (IOException e) {
                Log.e("SettingsActivity", "log clear error: " + e);
                runOnUiThread(() -> syslogsText.setText("Failed to clear logs: " + e.getMessage()));
            }
        });
        clearThread.start();
    }

    private void updateAppInfo() {
        try {
            String versionName = getPackageManager()
                    .getPackageInfo(getPackageName(), 0).versionName;
            appVersion.setText("Version " + versionName);
        } catch (PackageManager.NameNotFoundException e) {
            appVersion.setText("Version unknown");
        }
    }

    private void updateServiceStatus() {
        boolean isRunning = isServiceRunning();
        int startCount = SmsReceiverService.getServiceStartCount(this);

        if (isRunning) {
            chipServiceStatus.setText("Active");
            chipServiceStatus.setChipIconResource(R.drawable.ic_check_circle);
            serviceStatusText.setText("Service is running and monitoring messages • Started " + startCount + " times");
        } else {
            chipServiceStatus.setText("Inactive");
            chipServiceStatus.setChipIconResource(R.drawable.ic_error);
            serviceStatusText.setText("Service is not running");
        }

        // Check battery optimization status
        checkBatteryOptimization();
    }

    private void checkBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName())) {
                // Show warning about battery optimization
                showBatteryOptimizationWarning();
            }
        }
    }

    private void showBatteryOptimizationWarning() {
        // You can add a warning message or button to guide users to disable battery
        // optimization
        Log.w(TAG, "Battery optimization is enabled - this may affect background operation");
    }

    private void updateAllPermissionStatus() {
        // SMS Permission
        updatePermissionChip(chipSmsPermission,
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED);

        // Phone State Permission
        updatePermissionChip(chipPhoneStatePermission,
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED);

        // Call Log Permission
        updatePermissionChip(chipCallLogPermission,
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED);

        // Contacts Permission
        updatePermissionChip(chipContactsPermission,
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED);

        // Phone Numbers Permission
        updatePermissionChip(chipPhoneNumbersPermission,
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.READ_PHONE_NUMBERS) == PackageManager.PERMISSION_GRANTED);

        // WiFi Access Permission
        updatePermissionChip(chipWifiPermission,
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_WIFI_STATE) == PackageManager.PERMISSION_GRANTED);

        // Notifications Permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            updatePermissionChip(chipNotificationsPermission,
                    ContextCompat.checkSelfPermission(this,
                            Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED);
            chipNotificationsPermission.setVisibility(View.VISIBLE);
        } else {
            chipNotificationsPermission.setVisibility(View.GONE);
        }

        // Notification Listener Service
        updatePermissionChip(chipNotificationListenerPermission, isNotificationListenerEnabled());
    }

    private void updatePermissionChip(Chip chip, boolean isGranted) {
        if (isGranted) {
            chip.setText("Granted");
            chip.setChipBackgroundColorResource(R.color.md_theme_light_tertiaryContainer);
            chip.setTextColor(ContextCompat.getColor(this, R.color.md_theme_light_onTertiaryContainer));
        } else {
            chip.setText("Denied");
            chip.setChipBackgroundColorResource(R.color.md_theme_light_errorContainer);
            chip.setTextColor(ContextCompat.getColor(this, R.color.md_theme_light_onErrorContainer));
        }
    }

    private boolean isNotificationListenerEnabled() {
        ComponentName cn = new ComponentName(this, NotificationListenerService.class);
        String flat = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        return flat != null && flat.contains(cn.flattenToString());
    }

    private boolean isServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        if (manager != null) {
            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (SmsReceiverService.class.getName().equals(service.service.getClassName())) {
                    return true;
                }
            }
        }
        return false;
    }

    private void openOperatorSettings() {
        Intent intent = new Intent(this, OperatorSettingsActivity.class);
        startActivity(intent);
    }

    private void openAppWebhooks() {
        Intent intent = new Intent(this, AppWebhooksActivity.class);
        startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh status when returning to the activity
        updateServiceStatus();
        updateAllPermissionStatus();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Clear references to prevent memory leaks
        syslogsText = null;
        appVersion = null;
        serviceStatusText = null;
        aboutText = null;
        chipServiceStatus = null;
        chipSmsPermission = null;
        chipPhoneStatePermission = null;
        chipCallLogPermission = null;
        chipContactsPermission = null;
        chipPhoneNumbersPermission = null;
        chipNotificationsPermission = null;
        chipNotificationListenerPermission = null;
        chipWifiPermission = null;
        btnRefreshLogs = null;
        btnClearLogs = null;
        btnCopyLogs = null;
        btnOperatorSettings = null;
        btnAppWebhooks = null;

        Log.d(TAG, "SettingsActivity destroyed and cleaned up");
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);

        Log.d(TAG, "Memory trim requested: " + level);

        // Handle memory pressure
        switch (level) {
            case TRIM_MEMORY_UI_HIDDEN:
                // UI is hidden, can release UI-related resources
                break;
            case TRIM_MEMORY_RUNNING_MODERATE:
            case TRIM_MEMORY_RUNNING_LOW:
            case TRIM_MEMORY_RUNNING_CRITICAL:
                // App is running but system is low on memory
                performMemoryCleanup();
                break;
        }
    }

    private void performMemoryCleanup() {
        try {
            // Modern memory management - avoid System.gc()
            // Let the system handle garbage collection automatically

            Log.d(TAG, "Memory cleanup performed in SettingsActivity");
        } catch (Exception e) {
            Log.e(TAG, "Error during memory cleanup", e);
        }
    }
}