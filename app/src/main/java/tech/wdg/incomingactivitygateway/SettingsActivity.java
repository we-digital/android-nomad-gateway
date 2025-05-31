package tech.wdg.incomingactivitygateway;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;

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

    private TextView syslogsText;
    private TextView appVersion;
    private TextView serviceStatusText;
    private Chip chipServiceStatus;
    private Chip chipSmsPermission;
    private MaterialButton btnRefreshLogs;
    private MaterialButton btnClearLogs;

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
        loadSystemLogs();
        updateAppInfo();
        updateServiceStatus();
        updatePermissionStatus();
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
        chipServiceStatus = findViewById(R.id.chip_service_status);
        chipSmsPermission = findViewById(R.id.chip_sms_permission);
        btnRefreshLogs = findViewById(R.id.btn_refresh_logs);
        btnClearLogs = findViewById(R.id.btn_clear_logs);
    }

    private void setupClickListeners() {
        btnRefreshLogs.setOnClickListener(v -> loadSystemLogs());
        btnClearLogs.setOnClickListener(v -> clearSystemLogs());
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
        if (isRunning) {
            chipServiceStatus.setText("Active");
            chipServiceStatus.setChipIconResource(R.drawable.ic_check_circle);
            serviceStatusText.setText("Service is running and monitoring messages");
        } else {
            chipServiceStatus.setText("Inactive");
            chipServiceStatus.setChipIconResource(R.drawable.ic_error);
            serviceStatusText.setText("Service is not running");
        }
    }

    private void updatePermissionStatus() {
        boolean hasSmsPermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED;

        if (hasSmsPermission) {
            chipSmsPermission.setText("Granted");
            chipSmsPermission.setChipBackgroundColorResource(R.color.md_theme_light_tertiaryContainer);
        } else {
            chipSmsPermission.setText("Denied");
            chipSmsPermission.setChipBackgroundColorResource(R.color.md_theme_light_errorContainer);
        }
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
        updatePermissionStatus();
    }
}