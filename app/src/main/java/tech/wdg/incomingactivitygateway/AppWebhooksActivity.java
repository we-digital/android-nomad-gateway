package tech.wdg.incomingactivitygateway;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class AppWebhooksActivity extends AppCompatActivity {
    private static final String TAG = "AppWebhooksActivity";
    private static final String PREFS_NAME = "AppWebhooksPrefs";

    // Preference keys
    private static final String KEY_MANUAL_START_ENABLED = "manual_start_enabled";
    private static final String KEY_MANUAL_START_URL = "manual_start_url";
    private static final String KEY_AUTO_START_ENABLED = "auto_start_enabled";
    private static final String KEY_AUTO_START_URL = "auto_start_url";
    private static final String KEY_SIM_STATUS_ENABLED = "sim_status_enabled";
    private static final String KEY_SIM_STATUS_URL = "sim_status_url";

    // UI components
    private SwitchMaterial switchManualStart;
    private TextInputLayout tilManualStartUrl;
    private TextInputEditText etManualStartUrl;

    private SwitchMaterial switchAutoStart;
    private TextInputLayout tilAutoStartUrl;
    private TextInputEditText etAutoStartUrl;

    private SwitchMaterial switchSimStatus;
    private TextInputLayout tilSimStatusUrl;
    private TextInputEditText etSimStatusUrl;

    private MaterialButton btnSave;
    private MaterialButton btnTestWebhooks;

    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_webhooks);

        // Handle window insets for edge-to-edge
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            v.setPadding(0, insets.getInsets(WindowInsetsCompat.Type.systemBars()).top, 0, 0);
            return insets;
        });

        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        setupToolbar();
        initializeViews();
        loadPreferences();
        setupListeners();
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("App Webhooks");
        }
    }

    private void initializeViews() {
        // Manual start webhook
        switchManualStart = findViewById(R.id.switch_manual_start);
        tilManualStartUrl = findViewById(R.id.til_manual_start_url);
        etManualStartUrl = findViewById(R.id.et_manual_start_url);

        // Auto start webhook
        switchAutoStart = findViewById(R.id.switch_auto_start);
        tilAutoStartUrl = findViewById(R.id.til_auto_start_url);
        etAutoStartUrl = findViewById(R.id.et_auto_start_url);

        // SIM status webhook
        switchSimStatus = findViewById(R.id.switch_sim_status);
        tilSimStatusUrl = findViewById(R.id.til_sim_status_url);
        etSimStatusUrl = findViewById(R.id.et_sim_status_url);

        // Buttons
        btnSave = findViewById(R.id.btn_save);
        btnTestWebhooks = findViewById(R.id.btn_test_webhooks);
    }

    private void loadPreferences() {
        // Manual start
        switchManualStart.setChecked(preferences.getBoolean(KEY_MANUAL_START_ENABLED, false));
        etManualStartUrl.setText(preferences.getString(KEY_MANUAL_START_URL, ""));
        tilManualStartUrl.setEnabled(switchManualStart.isChecked());

        // Auto start
        switchAutoStart.setChecked(preferences.getBoolean(KEY_AUTO_START_ENABLED, false));
        etAutoStartUrl.setText(preferences.getString(KEY_AUTO_START_URL, ""));
        tilAutoStartUrl.setEnabled(switchAutoStart.isChecked());

        // SIM status
        switchSimStatus.setChecked(preferences.getBoolean(KEY_SIM_STATUS_ENABLED, false));
        etSimStatusUrl.setText(preferences.getString(KEY_SIM_STATUS_URL, ""));
        tilSimStatusUrl.setEnabled(switchSimStatus.isChecked());
    }

    private void setupListeners() {
        // Switch listeners
        switchManualStart.setOnCheckedChangeListener((buttonView, isChecked) -> {
            tilManualStartUrl.setEnabled(isChecked);
            if (!isChecked) {
                tilManualStartUrl.setError(null);
            }
        });

        switchAutoStart.setOnCheckedChangeListener((buttonView, isChecked) -> {
            tilAutoStartUrl.setEnabled(isChecked);
            if (!isChecked) {
                tilAutoStartUrl.setError(null);
            }
        });

        switchSimStatus.setOnCheckedChangeListener((buttonView, isChecked) -> {
            tilSimStatusUrl.setEnabled(isChecked);
            if (!isChecked) {
                tilSimStatusUrl.setError(null);
            }
        });

        // Button listeners
        btnSave.setOnClickListener(v -> saveWebhooks());
        btnTestWebhooks.setOnClickListener(v -> testWebhooks());
    }

    private void saveWebhooks() {
        if (!validateInputs()) {
            return;
        }

        SharedPreferences.Editor editor = preferences.edit();

        // Save manual start webhook
        editor.putBoolean(KEY_MANUAL_START_ENABLED, switchManualStart.isChecked());
        editor.putString(KEY_MANUAL_START_URL, etManualStartUrl.getText().toString().trim());

        // Save auto start webhook
        editor.putBoolean(KEY_AUTO_START_ENABLED, switchAutoStart.isChecked());
        editor.putString(KEY_AUTO_START_URL, etAutoStartUrl.getText().toString().trim());

        // Save SIM status webhook
        editor.putBoolean(KEY_SIM_STATUS_ENABLED, switchSimStatus.isChecked());
        editor.putString(KEY_SIM_STATUS_URL, etSimStatusUrl.getText().toString().trim());

        editor.apply();

        Toast.makeText(this, "Webhooks saved successfully", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Webhooks configuration saved");
    }

    private boolean validateInputs() {
        boolean isValid = true;

        // Validate manual start URL
        if (switchManualStart.isChecked()) {
            String url = etManualStartUrl.getText().toString().trim();
            if (TextUtils.isEmpty(url)) {
                tilManualStartUrl.setError("URL is required when enabled");
                isValid = false;
            } else if (!isValidUrl(url)) {
                tilManualStartUrl.setError("Please enter a valid URL");
                isValid = false;
            } else {
                tilManualStartUrl.setError(null);
            }
        }

        // Validate auto start URL
        if (switchAutoStart.isChecked()) {
            String url = etAutoStartUrl.getText().toString().trim();
            if (TextUtils.isEmpty(url)) {
                tilAutoStartUrl.setError("URL is required when enabled");
                isValid = false;
            } else if (!isValidUrl(url)) {
                tilAutoStartUrl.setError("Please enter a valid URL");
                isValid = false;
            } else {
                tilAutoStartUrl.setError(null);
            }
        }

        // Validate SIM status URL
        if (switchSimStatus.isChecked()) {
            String url = etSimStatusUrl.getText().toString().trim();
            if (TextUtils.isEmpty(url)) {
                tilSimStatusUrl.setError("URL is required when enabled");
                isValid = false;
            } else if (!isValidUrl(url)) {
                tilSimStatusUrl.setError("Please enter a valid URL");
                isValid = false;
            } else {
                tilSimStatusUrl.setError(null);
            }
        }

        return isValid;
    }

    private boolean isValidUrl(String url) {
        return url.startsWith("http://") || url.startsWith("https://");
    }

    private void testWebhooks() {
        if (!validateInputs()) {
            Toast.makeText(this, "Please fix validation errors first", Toast.LENGTH_SHORT).show();
            return;
        }

        int testsToRun = 0;

        if (switchManualStart.isChecked()) {
            testsToRun++;
            testWebhook("Manual Start", etManualStartUrl.getText().toString().trim());
        }

        if (switchAutoStart.isChecked()) {
            testsToRun++;
            testWebhook("Auto Start", etAutoStartUrl.getText().toString().trim());
        }

        if (switchSimStatus.isChecked()) {
            testsToRun++;
            testWebhook("SIM Status", etSimStatusUrl.getText().toString().trim());
        }

        if (testsToRun == 0) {
            Toast.makeText(this, "No webhooks enabled to test", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Testing " + testsToRun + " webhook(s)...", Toast.LENGTH_SHORT).show();
        }
    }

    private void testWebhook(String type, String url) {
        // Create test payload
        WebhookPayload payload = new WebhookPayload();
        payload.event = "test_" + type.toLowerCase().replace(" ", "_");
        payload.timestamp = System.currentTimeMillis();
        payload.deviceId = android.os.Build.MODEL;
        payload.message = "Test webhook for " + type;

        // Send webhook
        WebhookSender.sendWebhook(this, url, payload, new WebhookSender.WebhookCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    Toast.makeText(AppWebhooksActivity.this,
                            type + " webhook test successful", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(AppWebhooksActivity.this,
                            type + " webhook test failed: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Static methods to check webhook status from other parts of the app
    public static boolean isManualStartWebhookEnabled(android.content.Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getBoolean(KEY_MANUAL_START_ENABLED, false);
    }

    public static String getManualStartWebhookUrl(android.content.Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getString(KEY_MANUAL_START_URL, "");
    }

    public static boolean isAutoStartWebhookEnabled(android.content.Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getBoolean(KEY_AUTO_START_ENABLED, false);
    }

    public static String getAutoStartWebhookUrl(android.content.Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getString(KEY_AUTO_START_URL, "");
    }

    public static boolean isSimStatusWebhookEnabled(android.content.Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getBoolean(KEY_SIM_STATUS_ENABLED, false);
    }

    public static String getSimStatusWebhookUrl(android.content.Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getString(KEY_SIM_STATUS_URL, "");
    }
}