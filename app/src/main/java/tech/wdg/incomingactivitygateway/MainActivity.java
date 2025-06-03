package tech.wdg.incomingactivitygateway;

import android.Manifest;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.transition.platform.MaterialContainerTransformSharedElementCallback;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

public class MainActivity extends AppCompatActivity implements ForwardingRulesAdapter.OnRuleActionListener {

    private static final String TAG = "MainActivity";
    private Context context;
    private ForwardingRulesAdapter adapter;
    private ForwardingConfigDialog configDialog;
    private RecyclerView recyclerView;
    private LinearLayout emptyState;
    private Chip chipStatus;
    private Chip chipCount;
    private TextView infoNotice;

    // Filter chips
    private ChipGroup chipGroupFilter;
    private Chip chipFilterAll;
    private Chip chipFilterSms;
    private Chip chipFilterPush;
    private Chip chipFilterCalls;

    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int NOTIFICATION_LISTENER_REQUEST_CODE = 101;

    // Modern activity result launcher
    private ActivityResultLauncher<Intent> notificationSettingsLauncher;

    // All critical permissions the app needs
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_PHONE_NUMBERS,
            Manifest.permission.ACCESS_WIFI_STATE
    };

    // Additional permissions for Android 14+
    private static final String[] ANDROID_14_PERMISSIONS = {
            "android.permission.FOREGROUND_SERVICE_DATA_SYNC",
            "android.permission.FOREGROUND_SERVICE_SPECIAL_USE"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Enable shared element transitions
        setExitSharedElementCallback(new MaterialContainerTransformSharedElementCallback());
        getWindow().setSharedElementsUseOverlay(false);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize modern activity result launcher
        notificationSettingsLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    // Check if notification listener was enabled
                    if (isNotificationListenerEnabled()) {
                        showInfo("Notification access enabled! The app is now ready for push notification forwarding.");
                    } else {
                        showInfo("Notification access not enabled. Push notification forwarding will not work.");
                    }
                    // Initialize app regardless of result
                    initializeApp();
                });

        // Set up toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Initialize views
        initializeViews();

        // Check and request all necessary permissions
        checkAndRequestPermissions();

        // Trigger manual start webhook
        triggerManualStartWebhook();
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.recyclerView);
        emptyState = findViewById(R.id.empty_state);
        chipStatus = findViewById(R.id.chip_status);
        chipCount = findViewById(R.id.chip_count);
        infoNotice = findViewById(R.id.info_notice);

        // Initialize filter chips
        chipGroupFilter = findViewById(R.id.chip_group_filter);
        chipFilterAll = findViewById(R.id.chip_filter_all);
        chipFilterSms = findViewById(R.id.chip_filter_sms);
        chipFilterPush = findViewById(R.id.chip_filter_push);
        chipFilterCalls = findViewById(R.id.chip_filter_calls);
    }

    private void checkAndRequestPermissions() {
        List<String> permissionsToRequest = new ArrayList<>();

        // Check all required permissions
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }

        // Add notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        // Add foreground service permissions for Android 14+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            for (String permission : ANDROID_14_PERMISSIONS) {
                if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(permission);
                }
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            // Show explanation dialog before requesting permissions
            showPermissionExplanationDialog(permissionsToRequest);
        } else {
            // All regular permissions granted, check notification listener access
            checkNotificationListenerAccess();
        }
    }

    private void checkNotificationListenerAccess() {
        if (!isNotificationListenerEnabled()) {
            showNotificationListenerDialog();
        } else {
            // All permissions granted, proceed with app initialization
            initializeApp();
        }
    }

    private boolean isNotificationListenerEnabled() {
        ComponentName cn = new ComponentName(this, NotificationListenerService.class);
        String flat = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        return flat != null && flat.contains(cn.flattenToString());
    }

    private void showNotificationListenerDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Enable Notification Access")
                .setMessage(
                        "To forward push notifications from other apps, Activity Gateway needs special notification access.\n\n"
                                +
                                "This permission allows the app to read notifications from other apps and forward them to your configured webhooks.\n\n"
                                +
                                "You can enable this later in Settings if you prefer.")
                .setPositiveButton("Open Settings", (dialog, which) -> {
                    try {
                        Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
                        notificationSettingsLauncher.launch(intent);
                    } catch (Exception e) {
                        showInfo(
                                "Please enable notification access manually in Settings → Apps & notifications → Special app access → Notification access");
                        initializeApp();
                    }
                })
                .setNegativeButton("Skip", (dialog, which) -> {
                    showInfo(
                            "Push notification forwarding will not work without notification access. You can enable it later in Settings.");
                    initializeApp();
                })
                .setCancelable(false)
                .show();
    }

    private void showPermissionExplanationDialog(List<String> permissionsToRequest) {
        StringBuilder message = new StringBuilder();
        message.append("Activity Gateway needs several permissions to function properly:\n\n");
        message.append("• SMS Access - To receive and forward SMS messages\n");
        message.append("• Phone State - To identify SIM cards and monitor calls\n");
        message.append("• Call Log - To detect incoming calls\n");
        message.append("• Contacts - To resolve phone numbers to names\n");
        message.append("• Phone Numbers - To identify your phone numbers\n");
        message.append("• WiFi Access - To collect network information for webhooks\n");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            message.append("• Notifications - To show service status\n");
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            message.append("• Foreground Service - To run reliably in background\n");
        }

        message.append("\nYou can review and manage these permissions anytime in Settings.");

        new MaterialAlertDialogBuilder(this)
                .setTitle("Permissions Required")
                .setMessage(message.toString())
                .setPositiveButton("Grant Permissions", (dialog, which) -> {
                    requestPermissions(permissionsToRequest);
                })
                .setNegativeButton("Skip", (dialog, which) -> {
                    // Initialize app with limited functionality
                    showInfo(
                            "Some features may not work without required permissions. You can grant them later in Settings.");
                    initializeApp();
                })
                .setCancelable(false)
                .show();
    }

    private void requestPermissions(List<String> permissionsToRequest) {
        String[] permissionsArray = permissionsToRequest.toArray(new String[0]);
        ActivityCompat.requestPermissions(this, permissionsArray, PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            // Count granted permissions
            int grantedCount = 0;
            int deniedCount = 0;

            for (int result : grantResults) {
                if (result == PackageManager.PERMISSION_GRANTED) {
                    grantedCount++;
                } else {
                    deniedCount++;
                }
            }

            // Show result to user
            if (deniedCount == 0) {
                showInfo("All permissions granted! Checking notification access...");
                // Check notification listener access after regular permissions
                checkNotificationListenerAccess();
            } else if (grantedCount > 0) {
                showInfo(grantedCount
                        + " permissions granted. Some features may be limited. You can grant remaining permissions in Settings.");
                // Still check notification listener access
                checkNotificationListenerAccess();
            } else {
                showInfo("Permissions denied. You can grant them later in Settings for full functionality.");
                // Still check notification listener access
                checkNotificationListenerAccess();
            }
        }
    }

    private void initializeApp() {
        context = this;

        // Log background operation status for debugging
        BackgroundOperationManager.logBackgroundOperationStatus(this);

        // Initialize adapter with empty list initially
        adapter = new ForwardingRulesAdapter(this, new ArrayList<>(), this);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Setup filter chips
        setupFilterChips();

        // Setup FAB
        ExtendedFloatingActionButton fab = findViewById(R.id.btn_add);
        fab.setOnClickListener(view -> showAddDialog());

        // Add scroll listener to collapse/expand FAB
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0 && fab.isExtended()) {
                    fab.shrink();
                } else if (dy < 0 && !fab.isExtended()) {
                    fab.extend();
                }
            }
        });

        // Load and display rules
        refreshList();

        // Update service status
        updateServiceStatus();

        // Start service if not running
        if (!isServiceRunning()) {
            startService();
        }

        // Check and show background operation guidance if needed
        checkBackgroundOperationGuidance();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.action_bar_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_bar_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }

    private void updateEmptyState(int itemCount) {
        if (itemCount == 0) {
            emptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void updateChipCount(int count) {
        chipCount.setText(count + " Rules");
    }

    private void updateServiceStatus() {
        boolean isRunning = isServiceRunning();
        if (isRunning) {
            chipStatus.setText("Service Active");
            chipStatus.setChipIconResource(R.drawable.ic_check_circle);
        } else {
            chipStatus.setText("Service Inactive");
            chipStatus.setChipIconResource(R.drawable.ic_error);
        }
    }

    private boolean isServiceRunning() {
        // First check if service is expected to be running
        boolean expectedToRun = SmsReceiverService.isServiceExpectedToRun(this);

        // Then check if it's actually running
        try {
            ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
            if (manager != null) {
                // For API 26+, getRunningServices is limited to own services only
                // This is actually fine for our use case since we're checking our own service
                for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                    if (SmsReceiverService.class.getName()
                            .equals(service.service.getClassName())) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error checking service status via ActivityManager", e);
        }

        // If service is expected to run but not actually running, try to restart it
        if (expectedToRun) {
            Log.w(TAG, "Service expected to run but not found - attempting restart");
            startService();
            return false; // Return false for now, will be true after restart
        }

        return false;
    }

    private void startService() {
        Context appContext = getApplicationContext();
        Intent intent = new Intent(this, SmsReceiverService.class);

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                appContext.startForegroundService(intent);
            } else {
                appContext.startService(intent);
            }
            Log.d(TAG, "Service start requested");

            // Update status after a short delay to allow service to start
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                updateServiceStatus();
            }, 1000);

        } catch (Exception e) {
            Log.e(TAG, "Failed to start service", e);
            showInfo("Failed to start background service: " + e.getMessage());
        }
    }

    private void showInfo(String text) {
        if (infoNotice != null) {
            infoNotice.setText(text);
        }
    }

    private void showAddDialog() {
        Intent intent = new Intent(this, ForwardingRuleEditActivity.class);
        intent.putExtra(ForwardingRuleEditActivity.EXTRA_IS_NEW, true);
        startActivity(intent);
    }

    private void refreshList() {
        if (context == null) {
            Log.w(TAG, "Context is null, cannot refresh list");
            return;
        }

        try {
            ArrayList<ForwardingConfig> configs = ForwardingConfig.getAll(context);
            if (adapter != null) {
                adapter.updateRules(configs);
                updateEmptyState(configs.size());
                updateChipCount(configs.size());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error refreshing list", e);
            // Show empty state on error
            updateEmptyState(0);
            updateChipCount(0);
        }
    }

    // Implement ForwardingRulesAdapter.OnRuleActionListener
    @Override
    public void onRuleEdit(ForwardingConfig config) {
        Intent intent = new Intent(this, ForwardingRuleEditActivity.class);
        intent.putExtra(ForwardingRuleEditActivity.EXTRA_IS_NEW, false);
        intent.putExtra(ForwardingRuleEditActivity.EXTRA_CONFIG_KEY, config.getKey());
        startActivity(intent);
    }

    @Override
    public void onRuleDelete(ForwardingConfig config) {
        config.delete(context);
        refreshList();
    }

    @Override
    public void onRuleToggle(ForwardingConfig config, boolean enabled) {
        config.isOn = enabled;
        config.update(context);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh the list when returning from edit activity
        // Only refresh if the activity is properly initialized
        if (context != null && adapter != null) {
            refreshList();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Clean up resources to prevent memory leaks
        if (configDialog != null) {
            configDialog.cleanup();
            configDialog = null;
        }

        // Clear adapter references
        if (adapter != null) {
            adapter = null;
        }

        // Clear context reference
        context = null;

        Log.d(TAG, "MainActivity destroyed and cleaned up");
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);

        switch (level) {
            // Background memory trim levels (still supported)
            case TRIM_MEMORY_BACKGROUND:
            case TRIM_MEMORY_UI_HIDDEN:
                // App moved to background, perform light cleanup
                performMemoryCleanup();
                break;

            // Critical memory situations
            case TRIM_MEMORY_COMPLETE:
                // System is running very low on memory, aggressive cleanup
                performMemoryCleanup();
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
                break;

            default:
                // Handle other memory pressure levels
                performMemoryCleanup();
                break;
        }
    }

    private void performMemoryCleanup() {
        try {
            // Modern memory management - avoid System.gc()
            // Let the system handle garbage collection automatically

            // Clear any cached data in adapter
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }

            Log.d(TAG, "Memory cleanup performed in MainActivity");
        } catch (Exception e) {
            Log.e(TAG, "Error during memory cleanup", e);
        }
    }

    private void setupFilterChips() {
        chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                return; // Prevent deselecting all chips
            }

            int checkedId = checkedIds.get(0);

            if (checkedId == R.id.chip_filter_all) {
                adapter.clearFilter();
            } else if (checkedId == R.id.chip_filter_sms) {
                adapter.setFilter(ForwardingConfig.ActivityType.SMS);
            } else if (checkedId == R.id.chip_filter_push) {
                adapter.setFilter(ForwardingConfig.ActivityType.PUSH);
            } else if (checkedId == R.id.chip_filter_calls) {
                adapter.setFilter(ForwardingConfig.ActivityType.CALL);
            }

            // Update empty state based on filtered results
            updateEmptyState(adapter.getItemCount());
        });
    }

    private void checkBackgroundOperationGuidance() {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        boolean hasShownGuidance = prefs.getBoolean("has_shown_background_guidance", false);

        if (!hasShownGuidance && !BackgroundOperationManager.isBatteryOptimizationDisabled(this)) {
            showBackgroundOperationGuidance();
            prefs.edit().putBoolean("has_shown_background_guidance", true).apply();
        }
    }

    private void showBackgroundOperationGuidance() {
        String recommendations = BackgroundOperationManager.getBackgroundOperationRecommendations(this);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Background Operation Setup")
                .setMessage("To ensure reliable message forwarding, please configure your device settings:\n\n"
                        + recommendations)
                .setPositiveButton("Open Settings", (dialog, which) -> {
                    BackgroundOperationManager.openBatteryOptimizationSettings(this);
                })
                .setNegativeButton("Later", null)
                .setNeutralButton("App Settings", (dialog, which) -> {
                    BackgroundOperationManager.openAppSettings(this);
                })
                .show();
    }

    private void triggerManualStartWebhook() {
        // Check if manual start webhook is enabled
        if (AppWebhooksActivity.isManualStartWebhookEnabled(this)) {
            String url = AppWebhooksActivity.getManualStartWebhookUrl(this);
            if (!url.isEmpty()) {
                WebhookPayload payload = WebhookSender.createAppStartPayload(this, true);
                WebhookSender.sendWebhook(this, url, payload);
                Log.d(TAG, "Manual start webhook triggered");
            }
        }
    }
}