package tech.wdg.incomingactivitygateway;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class OperatorSettingsActivity extends AppCompatActivity {

    private static final String TAG = "OperatorSettings";
    private static final String PREFS_NAME = "operator_settings";
    private static final String KEY_ENABLE_SIM_INFO = "enable_sim_info";
    private static final String KEY_SIM_PREFIX = "sim_";
    private static final String KEY_SIM_NAME_SUFFIX = "_name";
    private static final String KEY_SIM_NUMBER_SUFFIX = "_number";

    private MaterialSwitch switchEnableSimInfo;
    private LinearLayout simCardsList;
    private MaterialButton btnRefreshSimInfo;
    private Chip chipPrivacyStatus;
    private Chip chipSimCount;

    private List<SimCardInfo> detectedSimCards;
    private SharedPreferences preferences;

    // Inner class to hold SIM card information
    private static class SimCardInfo {
        int slotIndex;
        String operatorName;
        String phoneNumber;
        String customName;
        String customNumber;
        boolean isActive;

        SimCardInfo(int slotIndex, String operatorName, String phoneNumber, boolean isActive) {
            this.slotIndex = slotIndex;
            this.operatorName = operatorName != null ? operatorName : "Unknown Operator";
            this.phoneNumber = phoneNumber != null ? phoneNumber : "Unknown Number";
            this.isActive = isActive;
            this.customName = "";
            this.customNumber = "";
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_operator_settings);

        // Handle window insets for edge-to-edge
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            v.setPadding(0, insets.getInsets(WindowInsetsCompat.Type.systemBars()).top, 0, 0);
            return insets;
        });

        setupToolbar();
        initializeViews();
        initializePreferences();
        setupClickListeners();
        loadSettings();
        refreshSimInformation();
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
        switchEnableSimInfo = findViewById(R.id.switch_enable_sim_info);
        simCardsList = findViewById(R.id.sim_cards_list);
        btnRefreshSimInfo = findViewById(R.id.btn_refresh_sim_info);
        chipPrivacyStatus = findViewById(R.id.chip_privacy_status);
        chipSimCount = findViewById(R.id.chip_sim_count);

        detectedSimCards = new ArrayList<>();
    }

    private void initializePreferences() {
        preferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private void setupClickListeners() {
        switchEnableSimInfo.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveSimInfoEnabled(isChecked);
            updatePrivacyStatus();
            updateSimCardsVisibility();
        });

        btnRefreshSimInfo.setOnClickListener(v -> refreshSimInformation());
    }

    private void loadSettings() {
        boolean simInfoEnabled = preferences.getBoolean(KEY_ENABLE_SIM_INFO, false);
        switchEnableSimInfo.setChecked(simInfoEnabled);
        updatePrivacyStatus();
        updateSimCardsVisibility();
    }

    private void saveSimInfoEnabled(boolean enabled) {
        preferences.edit()
                .putBoolean(KEY_ENABLE_SIM_INFO, enabled)
                .apply();
    }

    private void updatePrivacyStatus() {
        boolean enabled = switchEnableSimInfo.isChecked();
        if (enabled) {
            chipPrivacyStatus.setText("Disabled");
            chipPrivacyStatus.setChipBackgroundColorResource(R.color.md_theme_light_errorContainer);
        } else {
            chipPrivacyStatus.setText("Enabled");
            chipPrivacyStatus.setChipBackgroundColorResource(R.color.md_theme_light_tertiaryContainer);
        }
    }

    private void updateSimCardsVisibility() {
        boolean enabled = switchEnableSimInfo.isChecked();
        findViewById(R.id.sim_cards_container).setVisibility(enabled ? View.VISIBLE : View.GONE);
    }

    private void refreshSimInformation() {
        if (!hasPhonePermission()) {
            Toast.makeText(this, "Phone permission required to read SIM information", Toast.LENGTH_LONG).show();
            return;
        }

        detectedSimCards.clear();

        try {
            TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            SubscriptionManager subscriptionManager = (SubscriptionManager) getSystemService(
                    Context.TELEPHONY_SUBSCRIPTION_SERVICE);

            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                List<SubscriptionInfo> subscriptions = subscriptionManager.getActiveSubscriptionInfoList();

                if (subscriptions != null && !subscriptions.isEmpty()) {
                    for (SubscriptionInfo subscription : subscriptions) {
                        int slotIndex = subscription.getSimSlotIndex();
                        String operatorName = subscription.getCarrierName() != null
                                ? subscription.getCarrierName().toString()
                                : "Unknown Operator";

                        // Use getDisplayName() instead of deprecated getNumber()
                        String phoneNumber = "Unknown";
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            // For Android 13+, phone numbers are restricted
                            phoneNumber = "Protected";
                        } else {
                            // For older versions, try to get the number
                            try {
                                String number = subscription.getNumber();
                                if (number != null && !number.isEmpty()) {
                                    phoneNumber = number;
                                }
                            } catch (Exception e) {
                                Log.w(TAG, "Could not get phone number", e);
                            }
                        }

                        SimCardInfo simInfo = new SimCardInfo(slotIndex, operatorName, phoneNumber, true);
                        loadCustomSimSettings(simInfo);
                        detectedSimCards.add(simInfo);
                    }
                } else {
                    // Fallback for devices without active subscriptions
                    createFallbackSimCards(telephonyManager);
                }
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception reading SIM info", e);
            Toast.makeText(this, "Permission denied to read SIM information", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Error reading SIM info", e);
            Toast.makeText(this, "Error reading SIM information", Toast.LENGTH_SHORT).show();
        }

        updateSimCardsList();
        updateSimCount();
    }

    private void createFallbackSimCards(TelephonyManager telephonyManager) {
        // Create default SIM cards for dual SIM devices
        for (int i = 0; i < 2; i++) {
            SimCardInfo simInfo = new SimCardInfo(i, "SIM " + (i + 1), "Unknown", false);
            loadCustomSimSettings(simInfo);
            detectedSimCards.add(simInfo);
        }
    }

    private void loadCustomSimSettings(SimCardInfo simInfo) {
        String nameKey = KEY_SIM_PREFIX + simInfo.slotIndex + KEY_SIM_NAME_SUFFIX;
        String numberKey = KEY_SIM_PREFIX + simInfo.slotIndex + KEY_SIM_NUMBER_SUFFIX;

        simInfo.customName = preferences.getString(nameKey, "");
        simInfo.customNumber = preferences.getString(numberKey, "");
    }

    private void saveCustomSimSettings(SimCardInfo simInfo) {
        String nameKey = KEY_SIM_PREFIX + simInfo.slotIndex + KEY_SIM_NAME_SUFFIX;
        String numberKey = KEY_SIM_PREFIX + simInfo.slotIndex + KEY_SIM_NUMBER_SUFFIX;

        preferences.edit()
                .putString(nameKey, simInfo.customName)
                .putString(numberKey, simInfo.customNumber)
                .apply();
    }

    private void updateSimCardsList() {
        simCardsList.removeAllViews();

        for (SimCardInfo simInfo : detectedSimCards) {
            View simCardView = createSimCardView(simInfo);
            simCardsList.addView(simCardView);
        }
    }

    private View createSimCardView(SimCardInfo simInfo) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.item_sim_card, simCardsList, false);

        // Set SIM slot number
        TextView slotNumber = view.findViewById(R.id.sim_slot_number);
        slotNumber.setText(String.valueOf(simInfo.slotIndex + 1));

        // Set operator name
        TextView operatorName = view.findViewById(R.id.sim_operator_name);
        operatorName.setText(simInfo.operatorName);

        // Set phone number
        TextView phoneNumber = view.findViewById(R.id.sim_phone_number);
        phoneNumber.setText(simInfo.phoneNumber);

        // Set status chip
        Chip statusChip = view.findViewById(R.id.chip_sim_status);
        if (simInfo.isActive) {
            statusChip.setText("Active");
            statusChip.setChipBackgroundColorResource(R.color.md_theme_light_tertiaryContainer);
        } else {
            statusChip.setText("Inactive");
            statusChip.setChipBackgroundColorResource(R.color.md_theme_light_errorContainer);
        }

        // Set custom name input
        TextInputEditText customNameInput = view.findViewById(R.id.input_custom_name);
        customNameInput.setText(simInfo.customName);
        customNameInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                simInfo.customName = customNameInput.getText().toString().trim();
                saveCustomSimSettings(simInfo);
            }
        });

        // Set custom number input
        TextInputEditText customNumberInput = view.findViewById(R.id.input_custom_number);
        customNumberInput.setText(simInfo.customNumber);
        customNumberInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                simInfo.customNumber = customNumberInput.getText().toString().trim();
                saveCustomSimSettings(simInfo);
            }
        });

        return view;
    }

    private void updateSimCount() {
        int activeCount = 0;
        for (SimCardInfo simInfo : detectedSimCards) {
            if (simInfo.isActive) {
                activeCount++;
            }
        }

        chipSimCount.setText(activeCount + " detected");
    }

    private boolean hasPhonePermission() {
        return ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Public static methods for other activities to use
    public static boolean isSimInfoEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_ENABLE_SIM_INFO, false);
    }

    public static String getSimName(Context context, int slotIndex) {
        if (!isSimInfoEnabled(context)) {
            return "sim" + (slotIndex + 1);
        }

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String customName = prefs.getString(KEY_SIM_PREFIX + slotIndex + KEY_SIM_NAME_SUFFIX, "");

        if (!customName.isEmpty()) {
            return customName;
        }

        // Try to get operator name if available
        try {
            SubscriptionManager subscriptionManager = (SubscriptionManager) context
                    .getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
            if (ActivityCompat.checkSelfPermission(context,
                    Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                List<SubscriptionInfo> subscriptions = subscriptionManager.getActiveSubscriptionInfoList();
                if (subscriptions != null) {
                    for (SubscriptionInfo subscription : subscriptions) {
                        if (subscription.getSimSlotIndex() == slotIndex) {
                            return subscription.getCarrierName() != null ? subscription.getCarrierName().toString()
                                    : "sim" + (slotIndex + 1);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting SIM name", e);
        }

        return "sim" + (slotIndex + 1);
    }

    public static String getSimNumber(Context context, int slotIndex) {
        if (!isSimInfoEnabled(context)) {
            return "sim" + (slotIndex + 1);
        }

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String customNumber = prefs.getString(KEY_SIM_PREFIX + slotIndex + KEY_SIM_NUMBER_SUFFIX, "");

        if (!customNumber.isEmpty()) {
            return customNumber;
        }

        // Try to get actual phone number if available
        try {
            SubscriptionManager subscriptionManager = (SubscriptionManager) context
                    .getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
            if (ActivityCompat.checkSelfPermission(context,
                    Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                List<SubscriptionInfo> subscriptions = subscriptionManager.getActiveSubscriptionInfoList();
                if (subscriptions != null) {
                    for (SubscriptionInfo subscription : subscriptions) {
                        if (subscription.getSimSlotIndex() == slotIndex) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                // For Android 13+, phone numbers are restricted
                                return "sim" + (slotIndex + 1);
                            } else {
                                // For older versions, try to get the number
                                try {
                                    String number = subscription.getNumber();
                                    return (number != null && !number.isEmpty()) ? number : "sim" + (slotIndex + 1);
                                } catch (Exception e) {
                                    Log.w(TAG, "Could not get phone number", e);
                                    return "sim" + (slotIndex + 1);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting SIM number", e);
        }

        return "sim" + (slotIndex + 1);
    }
}