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
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.transition.platform.MaterialContainerTransformSharedElementCallback;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements ForwardingRulesAdapter.OnRuleActionListener {

    private Context context;
    private ForwardingRulesAdapter adapter;
    private ForwardingConfigDialog configDialog;
    private RecyclerView recyclerView;
    private LinearLayout emptyState;
    private Chip chipStatus;
    private Chip chipCount;
    private TextView infoNotice;

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

        if (id == R.id.action_bar_syslogs) {
            showSystemLogs();
        }

        return super.onOptionsItemSelected(item);
    }

    private void showSystemLogs() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        View view = getLayoutInflater().inflate(R.layout.syslogs, null);

        String logs = "";
        try {
            String[] command = new String[] {
                    "logcat", "-d", "*:E", "-m", "1000",
                    "|", "grep", "tech.wdg.incomingactivitygateway" };
            Process process = Runtime.getRuntime().exec(command);

            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                logs += line + "\n";
            }
        } catch (IOException ex) {
            logs = "getLog failed";
        }

        TextView logsTextContainer = view.findViewById(R.id.syslogs_text);
        logsTextContainer.setText(logs);

        TextView version = view.findViewById(R.id.syslogs_version);
        version.setText("v" + BuildConfig.VERSION_NAME);

        builder.setView(view);
        builder.setNegativeButton(R.string.btn_close, null);
        builder.setNeutralButton(R.string.btn_clear, null);

        final androidx.appcompat.app.AlertDialog dialog = builder.show();
        Objects.requireNonNull(dialog.getWindow())
                .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEUTRAL)
                .setOnClickListener(view1 -> {
                    String[] command = new String[] { "logcat", "-c" };
                    try {
                        Runtime.getRuntime().exec(command);
                    } catch (IOException e) {
                        Log.e("SmsGateway", "log clear error: " + e);
                    }
                    dialog.cancel();
                });
    }

    private void showList() {
        showInfo("Active and forwarding messages");
        updateServiceStatus();

        context = this;

        ArrayList<ForwardingConfig> configs = ForwardingConfig.getAll(context);

        // Set up RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);

        adapter = new ForwardingRulesAdapter(context, configs, this);
        recyclerView.setAdapter(adapter);

        // Update UI based on list size
        updateEmptyState(configs.size());
        updateChipCount(configs.size());

        // Set up FAB
        ExtendedFloatingActionButton fab = findViewById(R.id.btn_add);
        fab.setOnClickListener(v -> showAddDialog());

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

        if (!this.isServiceRunning()) {
            this.startService();
        }
    }

    private void updateEmptyState(int count) {
        if (count == 0) {
            emptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void updateChipCount(int count) {
        chipCount.setText(count + " " + (count == 1 ? "Rule" : "Rules"));
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
        if (configDialog != null) {
            configDialog.cleanup();
        }
        configDialog = new ForwardingConfigDialog(context, getLayoutInflater(), adapter);
        configDialog.showNew();
    }

    private void refreshList() {
        ArrayList<ForwardingConfig> configs = ForwardingConfig.getAll(context);
        adapter.updateRules(configs);
        updateEmptyState(configs.size());
        updateChipCount(configs.size());
    }

    // Implement ForwardingRulesAdapter.OnRuleActionListener
    @Override
    public void onRuleEdit(ForwardingConfig config) {
        if (configDialog != null) {
            configDialog.cleanup();
        }
        configDialog = new ForwardingConfigDialog(context, getLayoutInflater(), adapter);
        configDialog.showEdit(config);
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
    protected void onDestroy() {
        super.onDestroy();
        if (configDialog != null) {
            configDialog.cleanup();
        }
    }
}