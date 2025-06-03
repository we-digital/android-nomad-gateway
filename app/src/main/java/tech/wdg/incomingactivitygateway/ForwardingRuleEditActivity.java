package tech.wdg.incomingactivitygateway;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class ForwardingRuleEditActivity extends AppCompatActivity {

    public static final String EXTRA_CONFIG_KEY = "config_key";
    public static final String EXTRA_IS_NEW = "is_new";

    private ForwardingConfig config;
    private boolean isNew;

    // Basic fields
    private TextInputEditText urlInput;
    private TextInputEditText retriesInput;
    private MaterialSwitch ignoreSslSwitch;
    private MaterialSwitch chunkedModeSwitch;

    // Activity type selection
    private ChipGroup activityTypeChipGroup;
    private Chip chipTypeSms;
    private Chip chipTypePush;
    private Chip chipTypeCall;

    // App selection (for push notifications)
    private AutoCompleteTextView appSelectorDropdown;
    private LinearLayout appSelectorContainer;
    private String selectedAppPackage; // Store selected app package for push notifications

    // All sources switch and filtering
    private MaterialSwitch allSourcesSwitch;
    private LinearLayout sourceFilteringContainer;
    private LinearLayout smsPhoneContainer;

    // SMS phone numbers filtering (new)
    private LinearLayout smsPhoneNumbersContainer;
    private LinearLayout smsPhoneNumbersListContainer;
    private List<PhoneNumberFieldPair> smsPhoneNumberFields;

    // Call phone numbers filtering
    private LinearLayout callPhoneNumbersContainer;
    private LinearLayout phoneNumbersListContainer;
    private List<PhoneNumberFieldPair> phoneNumberFields;

    // Template fields - both key and value
    private TextInputEditText templateFromKeyInput;
    private TextInputEditText templateFromValueInput;
    private TextInputEditText templateTextKeyInput;
    private TextInputEditText templateTextValueInput;
    private TextInputEditText templateTimestampKeyInput;
    private TextInputEditText templateTimestampValueInput;
    private TextInputEditText templateSimKeyInput;
    private TextInputEditText templateSimValueInput;

    // Headers container
    private LinearLayout headersContainer;
    private List<HeaderFieldPair> headerFields;

    // Custom template fields
    private LinearLayout customTemplateFieldsContainer;
    private List<TemplateFieldPair> customTemplateFields;

    // Enhanced data configuration
    private MaterialSwitch enhancedDataEnabledSwitch;
    private LinearLayout enhancedDataOptionsContainer;
    private MaterialSwitch includeDeviceInfoSwitch;
    private MaterialSwitch includeSimInfoSwitch;
    private MaterialSwitch includeNetworkInfoSwitch;
    private MaterialSwitch includeAppConfigSwitch;

    // App data for dropdown
    private List<AppInfo> installedApps;

    // Template variables info
    private LinearLayout templateVariablesHeader;
    private LinearLayout templateVariablesContent;
    private ImageView templateVariablesExpandIcon;
    private boolean isTemplateVariablesExpanded = false;

    // Variable sections
    private LinearLayout smsVariablesSection;
    private LinearLayout pushVariablesSection;
    private LinearLayout callVariablesSection;

    // Inner class for app information
    private static class AppInfo {
        String name;
        String packageName;

        AppInfo(String name, String packageName) {
            this.name = name;
            this.packageName = packageName;
        }

        @Override
        public String toString() {
            return name + " (" + packageName + ")";
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forwarding_rule_edit);

        // Handle window insets for edge-to-edge
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            v.setPadding(0, insets.getInsets(WindowInsetsCompat.Type.systemBars()).top, 0, 0);
            return insets;
        });

        setupToolbar();
        initializeViews();
        loadInstalledApps();
        setupActivityTypeHandling();
        setupAllSourcesHandling();
        loadConfigData();
        setupClickListeners();
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
        // Basic fields
        urlInput = findViewById(R.id.input_url);
        retriesInput = findViewById(R.id.input_retries);
        ignoreSslSwitch = findViewById(R.id.switch_ignore_ssl);
        chunkedModeSwitch = findViewById(R.id.switch_chunked_mode);

        // Activity type selection
        activityTypeChipGroup = findViewById(R.id.activity_type_chip_group);
        chipTypeSms = findViewById(R.id.chip_type_sms);
        chipTypePush = findViewById(R.id.chip_type_push);
        chipTypeCall = findViewById(R.id.chip_type_call);

        // App selection (for push notifications)
        appSelectorDropdown = findViewById(R.id.app_selector_dropdown);
        appSelectorContainer = findViewById(R.id.app_selector_container);

        // All sources switch and filtering
        allSourcesSwitch = findViewById(R.id.switch_all_sources);
        sourceFilteringContainer = findViewById(R.id.source_filtering_container);
        smsPhoneContainer = findViewById(R.id.sms_phone_container);

        // SMS phone numbers filtering (similar to calls)
        smsPhoneNumbersContainer = findViewById(R.id.sms_phone_container); // Reuse same container
        smsPhoneNumbersListContainer = findViewById(R.id.sms_phone_numbers_list_container); // Need to add this to
                                                                                            // layout
        smsPhoneNumberFields = new ArrayList<>();

        // Call phone numbers filtering
        callPhoneNumbersContainer = findViewById(R.id.call_phone_numbers_container);
        phoneNumbersListContainer = findViewById(R.id.phone_numbers_list_container);
        phoneNumberFields = new ArrayList<>();

        // Template fields - both key and value
        templateFromKeyInput = findViewById(R.id.input_template_from_key);
        templateFromValueInput = findViewById(R.id.input_template_from_value);
        templateTextKeyInput = findViewById(R.id.input_template_text_key);
        templateTextValueInput = findViewById(R.id.input_template_text_value);
        templateTimestampKeyInput = findViewById(R.id.input_template_timestamp_key);
        templateTimestampValueInput = findViewById(R.id.input_template_timestamp_value);
        templateSimKeyInput = findViewById(R.id.input_template_sim_key);
        templateSimValueInput = findViewById(R.id.input_template_sim_value);

        // Headers
        headersContainer = findViewById(R.id.headers_container);
        headerFields = new ArrayList<>();

        // Custom template fields
        customTemplateFieldsContainer = findViewById(R.id.custom_template_fields_container);
        customTemplateFields = new ArrayList<>();

        // Add header button
        MaterialButton addHeaderButton = findViewById(R.id.btn_add_header);
        addHeaderButton.setOnClickListener(v -> addHeaderField("", ""));

        // Add custom template field button
        MaterialButton addTemplateFieldButton = findViewById(R.id.btn_add_template_field);
        addTemplateFieldButton.setOnClickListener(v -> addCustomTemplateField("", ""));

        // Add phone number button for calls
        MaterialButton addPhoneNumberButton = findViewById(R.id.btn_add_phone_number);
        addPhoneNumberButton.setOnClickListener(v -> addPhoneNumberField(""));

        // Add SMS phone number button
        MaterialButton addSmsPhoneNumberButton = findViewById(R.id.btn_add_sms_phone_number); // Need to add this to
                                                                                              // layout
        if (addSmsPhoneNumberButton != null) {
            addSmsPhoneNumberButton.setOnClickListener(v -> addSmsPhoneNumberField(""));
        }

        // Template variables info
        templateVariablesHeader = findViewById(R.id.template_variables_header);
        templateVariablesContent = findViewById(R.id.template_variables_content);
        templateVariablesExpandIcon = findViewById(R.id.template_variables_expand_icon);

        // Variable sections
        smsVariablesSection = findViewById(R.id.sms_variables_section);
        pushVariablesSection = findViewById(R.id.push_variables_section);
        callVariablesSection = findViewById(R.id.call_variables_section);

        // Enhanced data configuration
        enhancedDataEnabledSwitch = findViewById(R.id.switch_enhanced_data_enabled);
        enhancedDataOptionsContainer = findViewById(R.id.enhanced_data_options_container);
        includeDeviceInfoSwitch = findViewById(R.id.switch_include_device_info);
        includeSimInfoSwitch = findViewById(R.id.switch_include_sim_info);
        includeNetworkInfoSwitch = findViewById(R.id.switch_include_network_info);
        includeAppConfigSwitch = findViewById(R.id.switch_include_app_config);

        // Setup template variables expand/collapse
        setupTemplateVariablesExpandable();

        // Setup variable chip click listeners for copy functionality
        setupVariableChipClickListeners();

        // Setup enhanced data handling
        setupEnhancedDataHandling();
    }

    private void loadConfigData() {
        Intent intent = getIntent();
        isNew = intent.getBooleanExtra(EXTRA_IS_NEW, true);

        if (isNew) {
            config = new ForwardingConfig(this);
            setTitle("Add Forwarding Rule");
            setupDefaultValues();
        } else {
            String configKey = intent.getStringExtra(EXTRA_CONFIG_KEY);
            config = findConfigByKey(configKey);
            setTitle("Edit Forwarding Rule");
            populateFields();
        }
    }

    private ForwardingConfig findConfigByKey(String key) {
        ArrayList<ForwardingConfig> configs = ForwardingConfig.getAll(this);
        for (ForwardingConfig config : configs) {
            if (config.getKey().equals(key)) {
                return config;
            }
        }
        return new ForwardingConfig(this); // fallback
    }

    private void setupDefaultValues() {
        retriesInput.setText(String.valueOf(ForwardingConfig.getDefaultRetriesNumber()));
        chunkedModeSwitch.setChecked(true);

        // Enable "All sources" by default for new rules
        allSourcesSwitch.setChecked(true);
        sourceFilteringContainer.setVisibility(View.GONE);

        // Set default template values based on activity type
        setDefaultTemplateValues();

        // Add default header
        addHeaderField("User-Agent", "Android-activity-gateway App");
    }

    private void setDefaultTemplateValues() {
        boolean isPush = chipTypePush.isChecked();
        boolean isCall = chipTypeCall.isChecked();

        if (isPush) {
            // Default template values for push notifications
            templateFromKeyInput.setText("app");
            templateFromValueInput.setText("%package%");
            templateTextKeyInput.setText("message");
            templateTextValueInput.setText("%text%");
            templateTimestampKeyInput.setText("timestamp");
            templateTimestampValueInput.setText("%sentStamp%");
            templateSimKeyInput.setText("title");
            templateSimValueInput.setText("%title%");
        } else if (isCall) {
            // Default template values for calls
            templateFromKeyInput.setText("from");
            templateFromValueInput.setText("%from%");
            templateTextKeyInput.setText("contact");
            templateTextValueInput.setText("%contact%");
            templateTimestampKeyInput.setText("timestamp");
            templateTimestampValueInput.setText("%timestamp%");
            templateSimKeyInput.setText("sim");
            templateSimValueInput.setText("%sim%");
        } else {
            // Default template values for SMS
            templateFromKeyInput.setText("from");
            templateFromValueInput.setText("%from%");
            templateTextKeyInput.setText("text");
            templateTextValueInput.setText("%text%");
            templateTimestampKeyInput.setText("sentStamp");
            templateTimestampValueInput.setText("%sentStamp%");
            templateSimKeyInput.setText("sim");
            templateSimValueInput.setText("%sim%");
        }
    }

    private void populateFields() {
        // Handle "All sources" switch
        if ("*".equals(config.getSender())) {
            allSourcesSwitch.setChecked(true);
            sourceFilteringContainer.setVisibility(View.GONE);
            selectedAppPackage = null; // Clear app selection
        } else {
            allSourcesSwitch.setChecked(false);
            sourceFilteringContainer.setVisibility(View.VISIBLE);

            // Set the appropriate field based on activity type
            if (config.getActivityType() == ForwardingConfig.ActivityType.PUSH) {
                selectedAppPackage = config.getSender();
                // Find and set the app in dropdown
                for (int i = 0; i < installedApps.size(); i++) {
                    if (installedApps.get(i).packageName.equals(selectedAppPackage)) {
                        appSelectorDropdown.setText(installedApps.get(i).toString(), false);
                        break;
                    }
                }
            } else if (config.getActivityType() == ForwardingConfig.ActivityType.CALL) {
                // Don't set in text field, will be handled by parseExistingPhoneNumbers
            } else {
                // For SMS, don't set in text field, will be handled by
                // parseExistingSmsPhoneNumbers
            }
        }

        urlInput.setText(config.getUrl());
        retriesInput.setText(String.valueOf(config.getRetriesNumber()));
        ignoreSslSwitch.setChecked(config.getIgnoreSsl());
        chunkedModeSwitch.setChecked(config.getChunkedMode());

        // Set activity type
        if (config.getActivityType() == ForwardingConfig.ActivityType.PUSH) {
            chipTypePush.setChecked(true);
            appSelectorContainer.setVisibility(View.VISIBLE);
            updateUIForPushNotifications();
        } else if (config.getActivityType() == ForwardingConfig.ActivityType.CALL) {
            chipTypeCall.setChecked(true);
            updateUIForCalls();
        } else {
            chipTypeSms.setChecked(true);
            appSelectorContainer.setVisibility(View.GONE);
            updateUIForSMS();
        }

        // Parse existing template
        parseExistingTemplate();

        // Parse existing headers
        parseExistingHeaders();

        // Parse existing phone numbers for calls
        if (config.getActivityType() == ForwardingConfig.ActivityType.CALL && !allSourcesSwitch.isChecked()) {
            parseExistingPhoneNumbers();
        }

        // Parse existing phone numbers for SMS
        if (config.getActivityType() == ForwardingConfig.ActivityType.SMS && !allSourcesSwitch.isChecked()) {
            parseExistingSmsPhoneNumbers();
        }

        // Load enhanced data configuration
        enhancedDataEnabledSwitch.setChecked(config.isEnhancedDataEnabled());
        enhancedDataOptionsContainer.setVisibility(config.isEnhancedDataEnabled() ? View.VISIBLE : View.GONE);
        includeDeviceInfoSwitch.setChecked(config.isIncludeDeviceInfo());
        includeSimInfoSwitch.setChecked(config.isIncludeSimInfo());
        includeNetworkInfoSwitch.setChecked(config.isIncludeNetworkInfo());
        includeAppConfigSwitch.setChecked(config.isIncludeAppConfig());
    }

    private void parseExistingTemplate() {
        try {
            String template = config.getJsonTemplate();
            JSONObject json = new JSONObject(template);

            // Don't reset to defaults first - we'll handle fields manually
            // Clear any existing custom fields
            customTemplateFields.clear();
            customTemplateFieldsContainer.removeAllViews();

            // Track which standard template variables have been found
            boolean foundFrom = false, foundText = false, foundTimestamp = false, foundSim = false;
            boolean foundContact = false, foundDuration = false, foundTitle = false, foundContent = false,
                    foundPackage = false;

            Iterator<String> keys = json.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                String value = json.getString(key);

                // Check if it matches standard fields based on the value
                if (value.equals("%from%") && !foundFrom) {
                    templateFromKeyInput.setText(key);
                    templateFromValueInput.setText(value);
                    foundFrom = true;
                } else if ((value.equals("%text%") || value.equals("%content%") || value.equals("%message%"))
                        && !foundText) {
                    templateTextKeyInput.setText(key);
                    templateTextValueInput.setText(value);
                    foundText = true;
                } else if ((value.equals("%sentStamp%") || value.equals("%receivedStamp%")
                        || value.equals("%timestamp%")) && !foundTimestamp) {
                    templateTimestampKeyInput.setText(key);
                    templateTimestampValueInput.setText(value);
                    foundTimestamp = true;
                } else if (value.equals("%sim%") && !foundSim) {
                    templateSimKeyInput.setText(key);
                    templateSimValueInput.setText(value);
                    foundSim = true;
                } else if (value.equals("%contact%") && !foundContact) {
                    // For calls, contact goes in text field
                    if (config.getActivityType() == ForwardingConfig.ActivityType.CALL) {
                        templateTextKeyInput.setText(key);
                        templateTextValueInput.setText(value);
                        foundText = true;
                    }
                    foundContact = true;
                } else if (value.equals("%duration%") && !foundDuration) {
                    // For calls, duration should be a custom field now, not the standard sim field
                    foundDuration = true;
                    addCustomTemplateField(key, value);
                } else if (value.equals("%title%") && !foundTitle) {
                    // For push, title goes in sim field
                    if (config.getActivityType() == ForwardingConfig.ActivityType.PUSH) {
                        templateSimKeyInput.setText(key);
                        templateSimValueInput.setText(value);
                        foundSim = true;
                    }
                    foundTitle = true;
                } else if (value.equals("%package%") && !foundPackage) {
                    // For push, package goes in from field
                    if (config.getActivityType() == ForwardingConfig.ActivityType.PUSH) {
                        templateFromKeyInput.setText(key);
                        templateFromValueInput.setText(value);
                        foundFrom = true;
                    }
                    foundPackage = true;
                } else if (!isStandardTemplateValue(value)) {
                    // Only add as custom field if it's not a standard template variable
                    addCustomTemplateField(key, value);
                }
            }

            // Set defaults for any missing standard fields
            if (!foundFrom && templateFromKeyInput.getText().toString().isEmpty()) {
                if (config.getActivityType() == ForwardingConfig.ActivityType.PUSH) {
                    templateFromKeyInput.setText("app");
                    templateFromValueInput.setText("%package%");
                } else {
                    templateFromKeyInput.setText("from");
                    templateFromValueInput.setText("%from%");
                }
            }
            if (!foundText && templateTextKeyInput.getText().toString().isEmpty()) {
                if (config.getActivityType() == ForwardingConfig.ActivityType.PUSH) {
                    templateTextKeyInput.setText("message");
                    templateTextValueInput.setText("%text%");
                } else if (config.getActivityType() == ForwardingConfig.ActivityType.CALL) {
                    templateTextKeyInput.setText("contact");
                    templateTextValueInput.setText("%contact%");
                } else {
                    templateTextKeyInput.setText("text");
                    templateTextValueInput.setText("%text%");
                }
            }
            if (!foundTimestamp && templateTimestampKeyInput.getText().toString().isEmpty()) {
                if (config.getActivityType() == ForwardingConfig.ActivityType.CALL) {
                    templateTimestampKeyInput.setText("timestamp");
                    templateTimestampValueInput.setText("%timestamp%");
                } else {
                    templateTimestampKeyInput.setText("sentStamp");
                    templateTimestampValueInput.setText("%sentStamp%");
                }
            }
            if (!foundSim && templateSimKeyInput.getText().toString().isEmpty()) {
                if (config.getActivityType() == ForwardingConfig.ActivityType.PUSH) {
                    templateSimKeyInput.setText("title");
                    templateSimValueInput.setText("%title%");
                } else if (config.getActivityType() == ForwardingConfig.ActivityType.CALL) {
                    templateSimKeyInput.setText("sim");
                    templateSimValueInput.setText("%sim%");
                } else {
                    templateSimKeyInput.setText("sim");
                    templateSimValueInput.setText("%sim%");
                }
            }
        } catch (JSONException e) {
            // Fallback to defaults
            setDefaultTemplateValues();
        }
    }

    private boolean isStandardTemplateValue(String value) {
        return value.equals("%from%") || value.equals("%text%") || value.equals("%sentStamp%") ||
                value.equals("%receivedStamp%") || value.equals("%sim%") || value.equals("%timestamp%") ||
                value.equals("%duration%") || value.equals("%contact%") || value.equals("%title%") ||
                value.equals("%content%") || value.equals("%package%") || value.equals("%message%");
    }

    private void parseExistingHeaders() {
        try {
            String headers = config.getHeaders();
            JSONObject json = new JSONObject(headers);

            Iterator<String> keys = json.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                String value = json.getString(key);
                addHeaderField(key, value);
            }
        } catch (JSONException e) {
            // Add default header if parsing fails
            addHeaderField("User-Agent", "Android-activity-gateway App");
        }
    }

    private void addHeaderField(String key, String value) {
        View headerView = LayoutInflater.from(this).inflate(R.layout.item_header_field, headersContainer, false);

        TextInputEditText keyInput = headerView.findViewById(R.id.input_header_key);
        TextInputEditText valueInput = headerView.findViewById(R.id.input_header_value);
        MaterialButton removeButton = headerView.findViewById(R.id.btn_remove_header);

        keyInput.setText(key);
        valueInput.setText(value);

        HeaderFieldPair pair = new HeaderFieldPair(headerView, keyInput, valueInput);
        headerFields.add(pair);

        removeButton.setOnClickListener(v -> {
            headersContainer.removeView(headerView);
            headerFields.remove(pair);
        });

        headersContainer.addView(headerView);
    }

    private void addCustomTemplateField(String key, String value) {
        View templateView = LayoutInflater.from(this).inflate(R.layout.item_template_field,
                customTemplateFieldsContainer, false);

        TextInputEditText keyInput = templateView.findViewById(R.id.input_template_field_key);
        TextInputEditText valueInput = templateView.findViewById(R.id.input_template_field_value);
        MaterialButton removeButton = templateView.findViewById(R.id.btn_remove_template_field);

        keyInput.setText(key);
        valueInput.setText(value);

        TemplateFieldPair pair = new TemplateFieldPair(templateView, keyInput, valueInput);
        customTemplateFields.add(pair);

        removeButton.setOnClickListener(v -> {
            customTemplateFieldsContainer.removeView(templateView);
            customTemplateFields.remove(pair);
        });

        customTemplateFieldsContainer.addView(templateView);
    }

    private void addPhoneNumberField(String phoneNumber) {
        View phoneView = LayoutInflater.from(this).inflate(R.layout.item_phone_field,
                phoneNumbersListContainer, false);

        TextInputEditText phoneInput = phoneView.findViewById(R.id.input_phone_number);
        MaterialButton removeButton = phoneView.findViewById(R.id.btn_remove_phone);

        phoneInput.setText(phoneNumber);

        PhoneNumberFieldPair pair = new PhoneNumberFieldPair(phoneView, phoneInput);
        phoneNumberFields.add(pair);

        removeButton.setOnClickListener(v -> {
            phoneNumbersListContainer.removeView(phoneView);
            phoneNumberFields.remove(pair);
        });

        phoneNumbersListContainer.addView(phoneView);
    }

    private void addSmsPhoneNumberField(String phoneNumber) {
        // Create container if it doesn't exist yet
        if (smsPhoneNumbersListContainer == null) {
            // If the list container doesn't exist in layout, create it dynamically
            smsPhoneNumbersListContainer = new LinearLayout(this);
            smsPhoneNumbersListContainer.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            smsPhoneNumbersListContainer.setLayoutParams(params);

            // Add it to the SMS phone container
            if (smsPhoneContainer != null) {
                smsPhoneContainer.addView(smsPhoneNumbersListContainer);
            }
        }

        View phoneView = LayoutInflater.from(this).inflate(R.layout.item_phone_field,
                smsPhoneNumbersListContainer, false);

        TextInputEditText phoneInput = phoneView.findViewById(R.id.input_phone_number);
        MaterialButton removeButton = phoneView.findViewById(R.id.btn_remove_phone);

        phoneInput.setText(phoneNumber);

        PhoneNumberFieldPair pair = new PhoneNumberFieldPair(phoneView, phoneInput);
        smsPhoneNumberFields.add(pair);

        removeButton.setOnClickListener(v -> {
            smsPhoneNumbersListContainer.removeView(phoneView);
            smsPhoneNumberFields.remove(pair);
        });

        smsPhoneNumbersListContainer.addView(phoneView);
    }

    private void setupClickListeners() {
        MaterialButton saveButton = findViewById(R.id.btn_save);
        MaterialButton testButton = findViewById(R.id.btn_test);

        saveButton.setOnClickListener(v -> saveConfig());
        testButton.setOnClickListener(v -> testConfig());
    }

    private void saveConfig() {
        if (!validateInputs()) {
            return;
        }

        // Save basic fields
        if (allSourcesSwitch.isChecked()) {
            config.setSender("*");
        } else {
            if (chipTypePush.isChecked()) {
                // For push notifications, use selected app package
                config.setSender(selectedAppPackage != null ? selectedAppPackage : "");
            } else if (chipTypeCall.isChecked()) {
                // For calls, use comma-separated phone numbers
                config.setSender(buildPhoneNumbersList());
            } else {
                // For SMS, use comma-separated phone numbers from repeater field
                config.setSender(buildSmsPhoneNumbersList());
            }
        }
        config.setUrl(urlInput.getText().toString().trim());
        config.setRetriesNumber(Integer.parseInt(retriesInput.getText().toString()));
        config.setIgnoreSsl(ignoreSslSwitch.isChecked());
        config.setChunkedMode(chunkedModeSwitch.isChecked());

        // Set activity type based on selected chip
        if (chipTypePush.isChecked()) {
            config.setActivityType(ForwardingConfig.ActivityType.PUSH);
        } else if (chipTypeCall.isChecked()) {
            config.setActivityType(ForwardingConfig.ActivityType.CALL);
        } else {
            config.setActivityType(ForwardingConfig.ActivityType.SMS);
        }

        // Build JSON template
        config.setTemplate(buildJsonTemplate());

        // Build headers
        config.setHeaders(buildHeaders());

        // Enhanced data configuration
        config.setEnhancedDataEnabled(enhancedDataEnabledSwitch.isChecked());
        config.setIncludeDeviceInfo(includeDeviceInfoSwitch.isChecked());
        config.setIncludeSimInfo(includeSimInfoSwitch.isChecked());
        config.setIncludeNetworkInfo(includeNetworkInfoSwitch.isChecked());
        config.setIncludeAppConfig(includeAppConfigSwitch.isChecked());

        // Save to preferences
        config.save();

        Toast.makeText(this, isNew ? "Rule added successfully" : "Rule updated successfully",
                Toast.LENGTH_SHORT).show();

        finish();
    }

    private boolean validateInputs() {
        boolean isValid = true;

        // Validate sender only if "All sources" is not enabled
        if (!allSourcesSwitch.isChecked()) {
            if (chipTypePush.isChecked()) {
                // For push notifications, validate app selection
                if (selectedAppPackage == null || selectedAppPackage.isEmpty()) {
                    Toast.makeText(this, "Please select an app for push notifications", Toast.LENGTH_SHORT).show();
                    isValid = false;
                }
            } else if (chipTypeCall.isChecked()) {
                // For calls, validate phone numbers
                if (phoneNumberFields.isEmpty()) {
                    Toast.makeText(this, "Please add at least one phone number for call monitoring", Toast.LENGTH_SHORT)
                            .show();
                    isValid = false;
                } else {
                    // Validate each phone number
                    for (PhoneNumberFieldPair pair : phoneNumberFields) {
                        String phoneNumber = pair.phoneInput.getText().toString().trim();
                        if (!isValidPhoneNumber(phoneNumber)) {
                            pair.phoneInput.setError("Invalid phone number format");
                            isValid = false;
                        }
                    }
                }
            } else {
                // For SMS, validate phone numbers
                if (smsPhoneNumberFields.isEmpty()) {
                    Toast.makeText(this, "Please add at least one phone number for SMS monitoring", Toast.LENGTH_SHORT)
                            .show();
                    isValid = false;
                } else {
                    // Validate each SMS phone number
                    for (PhoneNumberFieldPair pair : smsPhoneNumberFields) {
                        String phoneNumber = pair.phoneInput.getText().toString().trim();
                        if (!isValidPhoneNumber(phoneNumber)) {
                            pair.phoneInput.setError("Invalid phone number format");
                            isValid = false;
                        }
                    }
                }
            }
        }

        // Validate URL
        String url = urlInput.getText().toString().trim();
        if (TextUtils.isEmpty(url)) {
            urlInput.setError("URL is required");
            isValid = false;
        } else {
            try {
                new URL(url);
            } catch (MalformedURLException e) {
                urlInput.setError("Invalid URL format");
                isValid = false;
            }
        }

        // Validate retries
        try {
            int retries = Integer.parseInt(retriesInput.getText().toString());
            if (retries < 0) {
                retriesInput.setError("Retries must be 0 or greater");
                isValid = false;
            }
        } catch (NumberFormatException e) {
            retriesInput.setError("Invalid number");
            isValid = false;
        }

        return isValid;
    }

    private String buildJsonTemplate() {
        try {
            JSONObject template = new JSONObject();

            // Add standard fields if they have both key and value
            addTemplateFieldIfValid(template, templateFromKeyInput, templateFromValueInput);
            addTemplateFieldIfValid(template, templateTextKeyInput, templateTextValueInput);
            addTemplateFieldIfValid(template, templateTimestampKeyInput, templateTimestampValueInput);
            addTemplateFieldIfValid(template, templateSimKeyInput, templateSimValueInput);

            // Add custom fields
            for (TemplateFieldPair pair : customTemplateFields) {
                String key = pair.keyInput.getText().toString().trim();
                String value = pair.valueInput.getText().toString().trim();

                if (!TextUtils.isEmpty(key) && !TextUtils.isEmpty(value)) {
                    template.put(key, value);
                }
            }

            return template.toString(2); // Pretty print with 2 spaces
        } catch (JSONException e) {
            return ForwardingConfig.getDefaultJsonTemplate();
        }
    }

    private void addTemplateFieldIfValid(JSONObject template, TextInputEditText keyInput, TextInputEditText valueInput)
            throws JSONException {
        String key = keyInput.getText().toString().trim();
        String value = valueInput.getText().toString().trim();

        if (!TextUtils.isEmpty(key) && !TextUtils.isEmpty(value)) {
            template.put(key, value);
        }
    }

    private String buildHeaders() {
        try {
            JSONObject headers = new JSONObject();

            for (HeaderFieldPair pair : headerFields) {
                String key = pair.keyInput.getText().toString().trim();
                String value = pair.valueInput.getText().toString().trim();

                if (!TextUtils.isEmpty(key) && !TextUtils.isEmpty(value)) {
                    headers.put(key, value);
                }
            }

            return headers.toString(2); // Pretty print with 2 spaces
        } catch (JSONException e) {
            return ForwardingConfig.getDefaultJsonHeaders();
        }
    }

    private void testConfig() {
        if (!validateInputs()) {
            return;
        }

        // Create temporary config for testing
        ForwardingConfig tempConfig = new ForwardingConfig(this);
        if (allSourcesSwitch.isChecked()) {
            tempConfig.setSender("*");
        } else {
            if (chipTypePush.isChecked()) {
                tempConfig.setSender(selectedAppPackage != null ? selectedAppPackage : "");
            } else if (chipTypeCall.isChecked()) {
                tempConfig.setSender(buildPhoneNumbersList());
            } else {
                tempConfig.setSender(buildSmsPhoneNumbersList());
            }
        }
        tempConfig.setUrl(urlInput.getText().toString().trim());
        tempConfig.setTemplate(buildJsonTemplate());
        tempConfig.setHeaders(buildHeaders());
        tempConfig.setIgnoreSsl(ignoreSslSwitch.isChecked());
        tempConfig.setChunkedMode(chunkedModeSwitch.isChecked());

        // Set activity type
        if (chipTypePush.isChecked()) {
            tempConfig.setActivityType(ForwardingConfig.ActivityType.PUSH);
        } else if (chipTypeCall.isChecked()) {
            tempConfig.setActivityType(ForwardingConfig.ActivityType.CALL);
        } else {
            tempConfig.setActivityType(ForwardingConfig.ActivityType.SMS);
        }

        // Test the configuration
        Thread testThread = new Thread(() -> {
            String payload;
            if (chipTypePush.isChecked()) {
                payload = tempConfig.prepareNotificationMessage(
                        "com.example.testapp", "Test Title", "Test Content", "Test Message",
                        System.currentTimeMillis());
            } else if (chipTypeCall.isChecked()) {
                // Use the new operator settings for SIM name
                String simName = OperatorSettingsActivity.getSimName(this, 0); // Use first SIM for testing
                payload = tempConfig.prepareCallMessage(
                        "+1234567890", "Test Contact", simName, System.currentTimeMillis());
            } else {
                // Use the new operator settings for SIM name
                String simName = OperatorSettingsActivity.getSimName(this, 0); // Use first SIM for testing
                payload = tempConfig.prepareMessage(
                        "123456789", "test message", simName, System.currentTimeMillis());
            }

            Request request = new Request(tempConfig.getUrl(), payload);
            request.setJsonHeaders(tempConfig.getHeaders());
            request.setIgnoreSsl(tempConfig.getIgnoreSsl());
            request.setUseChunkedMode(tempConfig.getChunkedMode());

            String result = request.execute();

            runOnUiThread(() -> {
                if (Request.RESULT_SUCCESS.equals(result)) {
                    Toast.makeText(this, "Test successful!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Test failed: " + result, Toast.LENGTH_LONG).show();
                }
            });
        });
        testThread.start();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private static class HeaderFieldPair {
        final View view;
        final TextInputEditText keyInput;
        final TextInputEditText valueInput;

        HeaderFieldPair(View view, TextInputEditText keyInput, TextInputEditText valueInput) {
            this.view = view;
            this.keyInput = keyInput;
            this.valueInput = valueInput;
        }
    }

    private static class TemplateFieldPair {
        final View view;
        final TextInputEditText keyInput;
        final TextInputEditText valueInput;

        TemplateFieldPair(View view, TextInputEditText keyInput, TextInputEditText valueInput) {
            this.view = view;
            this.keyInput = keyInput;
            this.valueInput = valueInput;
        }
    }

    private static class PhoneNumberFieldPair {
        final View view;
        final TextInputEditText phoneInput;

        PhoneNumberFieldPair(View view, TextInputEditText phoneInput) {
            this.view = view;
            this.phoneInput = phoneInput;
        }
    }

    private void loadInstalledApps() {
        installedApps = new ArrayList<>();
        PackageManager pm = getPackageManager();
        List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);

        for (ApplicationInfo app : apps) {
            // Skip system apps that don't typically send notifications
            if ((app.flags & ApplicationInfo.FLAG_SYSTEM) == 0 || isNotificationApp(app.packageName)) {
                String appName = pm.getApplicationLabel(app).toString();
                installedApps.add(new AppInfo(appName, app.packageName));
            }
        }

        // Sort apps alphabetically
        Collections.sort(installedApps, (a, b) -> a.name.compareToIgnoreCase(b.name));

        // Set up the dropdown adapter
        ArrayAdapter<AppInfo> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line,
                installedApps);
        appSelectorDropdown.setAdapter(adapter);

        // Handle app selection
        appSelectorDropdown.setOnItemClickListener((parent, view, position, id) -> {
            AppInfo selectedApp = (AppInfo) parent.getItemAtPosition(position);
            selectedAppPackage = selectedApp.packageName;
        });
    }

    private boolean isNotificationApp(String packageName) {
        // Common apps that send notifications
        return packageName.contains("whatsapp") ||
                packageName.contains("telegram") ||
                packageName.contains("gmail") ||
                packageName.contains("outlook") ||
                packageName.contains("slack") ||
                packageName.contains("discord") ||
                packageName.contains("twitter") ||
                packageName.contains("instagram") ||
                packageName.contains("facebook") ||
                packageName.contains("messenger") ||
                packageName.contains("signal") ||
                packageName.contains("viber");
    }

    private void setupActivityTypeHandling() {
        activityTypeChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.contains(R.id.chip_type_push)) {
                // Show app selector for push notifications
                appSelectorContainer.setVisibility(View.VISIBLE);
                updateUIForPushNotifications();
            } else if (checkedIds.contains(R.id.chip_type_call)) {
                // Show phone numbers for calls
                updateUIForCalls();
            } else {
                // Hide app selector for SMS
                appSelectorContainer.setVisibility(View.GONE);
                updateUIForSMS();
            }
        });
    }

    private void updateUIForPushNotifications() {
        // Show app selector for push notifications (only if not using "All sources")
        if (!allSourcesSwitch.isChecked()) {
            appSelectorContainer.setVisibility(View.VISIBLE);
        } else {
            appSelectorContainer.setVisibility(View.GONE);
        }

        // Hide SMS phone number field for push notifications
        smsPhoneContainer.setVisibility(View.GONE);

        // Hide call phone numbers container
        callPhoneNumbersContainer.setVisibility(View.GONE);

        // Update template variables info
        updateTemplateVariablesInfo(true, false);

        // Update template defaults if this is a new rule
        if (isNew) {
            setDefaultTemplateValues();
        }
    }

    private void updateUIForSMS() {
        // Hide app selector for SMS
        appSelectorContainer.setVisibility(View.GONE);

        // Show SMS phone number field (only if not using "All sources")
        if (!allSourcesSwitch.isChecked()) {
            smsPhoneContainer.setVisibility(View.VISIBLE);
            // Initialize SMS phone fields if empty
            if (smsPhoneNumberFields.isEmpty() && isNew) {
                addSmsPhoneNumberField("");
            }
        } else {
            smsPhoneContainer.setVisibility(View.GONE);
        }

        // Hide call phone numbers container
        callPhoneNumbersContainer.setVisibility(View.GONE);

        // Update template variables info
        updateTemplateVariablesInfo(false, false);

        // Update template defaults if this is a new rule
        if (isNew) {
            setDefaultTemplateValues();
        }
    }

    private void updateUIForCalls() {
        // Hide app selector and SMS phone container for calls
        appSelectorContainer.setVisibility(View.GONE);
        smsPhoneContainer.setVisibility(View.GONE);

        // Show call phone numbers container (only if not using "All sources")
        if (!allSourcesSwitch.isChecked()) {
            callPhoneNumbersContainer.setVisibility(View.VISIBLE);
        } else {
            callPhoneNumbersContainer.setVisibility(View.GONE);
        }

        // Update template variables info
        updateTemplateVariablesInfo(false, true);

        // Update template defaults if this is a new rule
        if (isNew) {
            setDefaultTemplateValues();
            // Only add a default phone number field if "All sources" is disabled
            if (!allSourcesSwitch.isChecked() && phoneNumberFields.isEmpty()) {
                addPhoneNumberField("");
            }
        }
    }

    private void updateTemplateVariablesInfo(boolean isPush, boolean isCall) {
        if (smsVariablesSection != null && pushVariablesSection != null && callVariablesSection != null) {
            // Hide all sections first
            smsVariablesSection.setVisibility(View.GONE);
            pushVariablesSection.setVisibility(View.GONE);
            callVariablesSection.setVisibility(View.GONE);

            // Show appropriate section based on activity type
            if (isPush) {
                pushVariablesSection.setVisibility(View.VISIBLE);
            } else if (isCall) {
                callVariablesSection.setVisibility(View.VISIBLE);
            } else {
                smsVariablesSection.setVisibility(View.VISIBLE);
            }
        }
    }

    private void setupAllSourcesHandling() {
        allSourcesSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // Hide source filtering fields when "All" is enabled
                sourceFilteringContainer.setVisibility(View.GONE);
            } else {
                // Show source filtering fields when "All" is disabled
                sourceFilteringContainer.setVisibility(View.VISIBLE);

                // Update container visibility based on activity type
                if (chipTypePush.isChecked()) {
                    appSelectorContainer.setVisibility(View.VISIBLE);
                    smsPhoneContainer.setVisibility(View.GONE);
                    callPhoneNumbersContainer.setVisibility(View.GONE);
                } else if (chipTypeCall.isChecked()) {
                    appSelectorContainer.setVisibility(View.GONE);
                    smsPhoneContainer.setVisibility(View.GONE);
                    callPhoneNumbersContainer.setVisibility(View.VISIBLE);
                } else {
                    appSelectorContainer.setVisibility(View.GONE);
                    smsPhoneContainer.setVisibility(View.VISIBLE);
                    callPhoneNumbersContainer.setVisibility(View.GONE);
                }
            }
        });
    }

    private String buildPhoneNumbersList() {
        StringBuilder phoneNumbers = new StringBuilder();
        for (PhoneNumberFieldPair pair : phoneNumberFields) {
            String phone = pair.phoneInput.getText().toString().trim();
            if (!TextUtils.isEmpty(phone)) {
                if (phoneNumbers.length() > 0) {
                    phoneNumbers.append(",");
                }
                phoneNumbers.append(phone);
            }
        }
        return phoneNumbers.toString();
    }

    private String buildSmsPhoneNumbersList() {
        StringBuilder phoneNumbers = new StringBuilder();
        for (PhoneNumberFieldPair pair : smsPhoneNumberFields) {
            String phone = pair.phoneInput.getText().toString().trim();
            if (!TextUtils.isEmpty(phone)) {
                if (phoneNumbers.length() > 0) {
                    phoneNumbers.append(",");
                }
                phoneNumbers.append(phone);
            }
        }
        return phoneNumbers.toString();
    }

    private void parseExistingPhoneNumbers() {
        String sender = config.getSender();
        if (!TextUtils.isEmpty(sender) && !sender.equals("*")) {
            String[] phoneNumbers = sender.split(",");
            for (String phoneNumber : phoneNumbers) {
                String trimmed = phoneNumber.trim();
                if (!TextUtils.isEmpty(trimmed)) {
                    addPhoneNumberField(trimmed);
                }
            }
        }
    }

    private void parseExistingSmsPhoneNumbers() {
        String sender = config.getSender();
        if (!TextUtils.isEmpty(sender) && !sender.equals("*")) {
            String[] phoneNumbers = sender.split(",");
            for (String phoneNumber : phoneNumbers) {
                String trimmed = phoneNumber.trim();
                if (!TextUtils.isEmpty(trimmed)) {
                    addSmsPhoneNumberField(trimmed);
                }
            }
        }
    }

    private boolean isValidPhoneNumber(String phoneNumber) {
        if (TextUtils.isEmpty(phoneNumber)) {
            return false;
        }
        // Basic phone number validation - allows digits, +, -, (, ), and spaces
        return phoneNumber.matches("^[+]?[0-9\\s\\-\\(\\)]+$") && phoneNumber.replaceAll("[^0-9]", "").length() >= 7;
    }

    private void setupTemplateVariablesExpandable() {
        templateVariablesHeader.setOnClickListener(v -> {
            isTemplateVariablesExpanded = !isTemplateVariablesExpanded;

            if (isTemplateVariablesExpanded) {
                // Expand with animation
                templateVariablesContent.setVisibility(View.VISIBLE);
                templateVariablesExpandIcon.setImageResource(R.drawable.ic_expand_less);

                // Animate the icon rotation
                templateVariablesExpandIcon.animate()
                        .rotation(180f)
                        .setDuration(200)
                        .start();

            } else {
                // Collapse with animation
                templateVariablesContent.setVisibility(View.GONE);
                templateVariablesExpandIcon.setImageResource(R.drawable.ic_expand_more);

                // Animate the icon rotation
                templateVariablesExpandIcon.animate()
                        .rotation(0f)
                        .setDuration(200)
                        .start();
            }
        });
    }

    private void setupVariableChipClickListeners() {
        // Find all variable chips and set click listeners
        setupChipClickListener(smsVariablesSection);
        setupChipClickListener(pushVariablesSection);
        setupChipClickListener(callVariablesSection);
    }

    private void setupChipClickListener(LinearLayout section) {
        if (section != null) {
            // Find ChipGroup within the section
            for (int i = 0; i < section.getChildCount(); i++) {
                View child = section.getChildAt(i);
                if (child instanceof com.google.android.material.chip.ChipGroup) {
                    com.google.android.material.chip.ChipGroup chipGroup = (com.google.android.material.chip.ChipGroup) child;

                    // Set touch listener for each chip to prevent default behavior
                    for (int j = 0; j < chipGroup.getChildCount(); j++) {
                        View chipChild = chipGroup.getChildAt(j);
                        if (chipChild instanceof com.google.android.material.chip.Chip) {
                            com.google.android.material.chip.Chip chip = (com.google.android.material.chip.Chip) chipChild;

                            // Completely disable all default behaviors
                            chip.setClickable(false);
                            chip.setFocusable(false);
                            chip.setLongClickable(false);
                            chip.setContextClickable(false);

                            // Remove any existing click listeners
                            chip.setOnClickListener(null);
                            chip.setOnLongClickListener(null);

                            // Use touch listener to handle copy action with visual feedback
                            chip.setOnTouchListener((v, event) -> {
                                switch (event.getAction()) {
                                    case android.view.MotionEvent.ACTION_DOWN:
                                        // Provide visual feedback on press
                                        v.setAlpha(0.7f);
                                        return true;
                                    case android.view.MotionEvent.ACTION_UP:
                                        // Reset visual state and copy to clipboard
                                        v.setAlpha(1.0f);
                                        copyToClipboard(chip.getText().toString());
                                        return true;
                                    case android.view.MotionEvent.ACTION_CANCEL:
                                        // Reset visual state if touch is cancelled
                                        v.setAlpha(1.0f);
                                        return true;
                                }
                                return true; // Consume all touch events
                            });
                        }
                    }
                }
            }
        }
    }

    private void copyToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Template Variable", text);
        clipboard.setPrimaryClip(clip);

        Toast.makeText(this, "Copied " + text + " to clipboard", Toast.LENGTH_SHORT).show();
    }

    private void setupEnhancedDataHandling() {
        // Handle enhanced data master switch
        enhancedDataEnabledSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            enhancedDataOptionsContainer.setVisibility(isChecked ? View.VISIBLE : View.GONE);

            // If disabling enhanced data, also disable all sub-options
            if (!isChecked) {
                includeDeviceInfoSwitch.setChecked(false);
                includeSimInfoSwitch.setChecked(false);
                includeNetworkInfoSwitch.setChecked(false);
                includeAppConfigSwitch.setChecked(false);
            }
        });
    }
}