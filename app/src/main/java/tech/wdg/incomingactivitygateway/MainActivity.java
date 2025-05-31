package tech.wdg.incomingactivitygateway;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
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
import java.util.Objects;

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

    private static final int PERMISSION_CODE = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Enable shared element transitions
        setExitSharedElementCallback(new MaterialContainerTransformSharedElementCallback());
        getWindow().setSharedElementsUseOverlay(false);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set up toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Initialize views
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

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.RECEIVE_SMS }, PERMISSION_CODE);
        } else {
            showList();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode != PERMISSION_CODE) {
            return;
        }
        for (int i = 0; i < permissions.length; i++) {
            if (!permissions[i].equals(Manifest.permission.RECEIVE_SMS)) {
                continue;
            }

            if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                showList();
            } else {
                showInfo(getResources().getString(R.string.permission_needed));
                emptyState.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            }

            return;
        }
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

    private void showList() {
        context = this;

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
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (tech.wdg.incomingactivitygateway.SmsReceiverService.class.getName()
                    .equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void startService() {
        Context appContext = getApplicationContext();
        Intent intent = new Intent(this, SmsReceiverService.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            appContext.startForegroundService(intent);
        } else {
            appContext.startService(intent);
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
}