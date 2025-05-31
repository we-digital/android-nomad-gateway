package tech.wdg.incomingactivitygateway;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ForwardingRuleEditActivity extends AppCompatActivity {

    public static final String EXTRA_CONFIG_KEY = "config_key";
    public static final String EXTRA_IS_NEW = "is_new";

    private ForwardingConfig config;
    private boolean isNew;

    // Basic fields
    private TextInputEditText senderInput;
    private TextInputEditText urlInput;
    private TextInputEditText retriesInput;
    private MaterialSwitch ignoreSslSwitch;
    private MaterialSwitch chunkedModeSwitch;

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
        senderInput = findViewById(R.id.input_sender);
        urlInput = findViewById(R.id.input_url);
        retriesInput = findViewById(R.id.input_retries);
        ignoreSslSwitch = findViewById(R.id.switch_ignore_ssl);
        chunkedModeSwitch = findViewById(R.id.switch_chunked_mode);

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

        // Default template values - both keys and values
        templateFromKeyInput.setText("from");
        templateFromValueInput.setText("%from%");
        templateTextKeyInput.setText("text");
        templateTextValueInput.setText("%text%");
        templateTimestampKeyInput.setText("sentStamp");
        templateTimestampValueInput.setText("%sentStamp%");
        templateSimKeyInput.setText("sim");
        templateSimValueInput.setText("%sim%");

        // Add default header
        addHeaderField("User-Agent", "SMS Forwarder App");
    }

    private void populateFields() {
        senderInput.setText(config.getSender());
        urlInput.setText(config.getUrl());
        retriesInput.setText(String.valueOf(config.getRetriesNumber()));
        ignoreSslSwitch.setChecked(config.getIgnoreSsl());
        chunkedModeSwitch.setChecked(config.getChunkedMode());

        // Parse existing template
        parseExistingTemplate();

        // Parse existing headers
        parseExistingHeaders();
    }

    private void parseExistingTemplate() {
        try {
            String template = config.getJsonTemplate();
            JSONObject json = new JSONObject(template);

            // Reset to defaults first
            setupDefaultValues();

            // Parse all fields from existing template
            Iterator<String> keys = json.keys();
            boolean foundFrom = false, foundText = false, foundTimestamp = false, foundSim = false;

            while (keys.hasNext()) {
                String key = keys.next();
                String value = json.getString(key);

                // Check if it matches standard fields
                if (value.equals("%from%") && !foundFrom) {
                    templateFromKeyInput.setText(key);
                    templateFromValueInput.setText(value);
                    foundFrom = true;
                } else if (value.equals("%text%") && !foundText) {
                    templateTextKeyInput.setText(key);
                    templateTextValueInput.setText(value);
                    foundText = true;
                } else if ((value.equals("%sentStamp%") || value.equals("%receivedStamp%")) && !foundTimestamp) {
                    templateTimestampKeyInput.setText(key);
                    templateTimestampValueInput.setText(value);
                    foundTimestamp = true;
                } else if (value.equals("%sim%") && !foundSim) {
                    templateSimKeyInput.setText(key);
                    templateSimValueInput.setText(value);
                    foundSim = true;
                } else {
                    // Add as custom field
                    addCustomTemplateField(key, value);
                }
            }
        } catch (JSONException e) {
            // Fallback to defaults
            setupDefaultValues();
        }
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
            addHeaderField("User-Agent", "SMS Forwarder App");
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
        config.setSender(senderInput.getText().toString().trim());
        config.setUrl(urlInput.getText().toString().trim());
        config.setRetriesNumber(Integer.parseInt(retriesInput.getText().toString()));
        config.setIgnoreSsl(ignoreSslSwitch.isChecked());
        config.setChunkedMode(chunkedModeSwitch.isChecked());

        // Build JSON template
        config.setTemplate(buildJsonTemplate());

        // Build headers
        config.setHeaders(buildHeaders());

        // Save to preferences
        config.save();

        Toast.makeText(this, isNew ? "Rule added successfully" : "Rule updated successfully",
                Toast.LENGTH_SHORT).show();

        finish();
    }

    private boolean validateInputs() {
        boolean isValid = true;

        // Validate sender
        if (TextUtils.isEmpty(senderInput.getText())) {
            senderInput.setError("Sender is required");
            isValid = false;
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
        tempConfig.setSender(senderInput.getText().toString().trim());
        tempConfig.setUrl(urlInput.getText().toString().trim());
        tempConfig.setTemplate(buildJsonTemplate());
        tempConfig.setHeaders(buildHeaders());
        tempConfig.setIgnoreSsl(ignoreSslSwitch.isChecked());
        tempConfig.setChunkedMode(chunkedModeSwitch.isChecked());

        // Test the configuration
        Thread testThread = new Thread(() -> {
            String payload = tempConfig.prepareMessage(
                    "123456789", "test message", "sim1", System.currentTimeMillis());
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
}