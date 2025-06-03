package tech.wdg.incomingactivitygateway;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.apache.commons.text.StringEscapeUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;

public class ForwardingConfig {
    final private Context context;

    // Activity types
    public enum ActivityType {
        SMS("sms"),
        PUSH("push"),
        CALL("call");

        private final String value;

        ActivityType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static ActivityType fromString(String value) {
            for (ActivityType type : ActivityType.values()) {
                if (type.value.equals(value)) {
                    return type;
                }
            }
            return SMS; // Default fallback
        }
    }

    private static final String KEY_KEY = "key";
    private static final String KEY_SENDER = "sender";
    private static final String KEY_URL = "url";
    private static final String KEY_SIM_SLOT = "sim_slot";
    private static final String KEY_TEMPLATE = "template";
    private static final String KEY_HEADERS = "headers";
    private static final String KEY_RETRIES_NUMBER = "retriesNumber";
    private static final String KEY_IGNORE_SSL = "ignoreSsl";
    private static final String KEY_CHUNKED_MODE = "chunkedMode";
    private static final String KEY_IS_SMS_ENABLED = "isSmsEnabled";
    private static final String KEY_IS_NOTIFICATION_ENABLED = "isNotificationEnabled";
    private static final String KEY_ACTIVITY_TYPE = "activityType";

    // Enhanced data configuration keys
    private static final String KEY_ENHANCED_DATA_ENABLED = "enhancedDataEnabled";
    private static final String KEY_INCLUDE_DEVICE_INFO = "includeDeviceInfo";
    private static final String KEY_INCLUDE_SIM_INFO = "includeSimInfo";
    private static final String KEY_INCLUDE_NETWORK_INFO = "includeNetworkInfo";
    private static final String KEY_INCLUDE_APP_CONFIG = "includeAppConfig";

    public long id;
    public boolean isOn = true;

    private String key;
    public String sender;
    public String url;
    public int simSlot = 0; // 0 means any
    public String template;
    public String headers;
    public int retriesNumber;
    public boolean ignoreSsl = false;
    public boolean chunkedMode = true;
    public boolean isSmsEnabled = true;
    public boolean isNotificationEnabled;
    public ActivityType activityType = ActivityType.SMS;

    // Enhanced data configuration
    public boolean enhancedDataEnabled = false;
    public boolean includeDeviceInfo = false;
    public boolean includeSimInfo = false;
    public boolean includeNetworkInfo = false;
    public boolean includeAppConfig = false;

    public ForwardingConfig(Context context) {
        this.context = context;
        this.id = System.currentTimeMillis() + new Random().nextInt(1000);
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getKey() {
        return this.key;
    }

    public String getSender() {
        return this.sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getUrl() {
        return this.url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getSimSlot() {
        return this.simSlot;
    }

    public void setSimSlot(int simSlot) {
        this.simSlot = simSlot;
    }

    public String getTemplate() {
        return this.template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public String getHeaders() {
        return this.headers;
    }

    public void setHeaders(String headers) {
        this.headers = headers;
    }

    public int getRetriesNumber() {
        return this.retriesNumber;
    }

    public void setRetriesNumber(int retriesNumber) {
        this.retriesNumber = retriesNumber;
    }

    public boolean getIgnoreSsl() {
        return this.ignoreSsl;
    }

    public void setIgnoreSsl(boolean ignoreSsl) {
        this.ignoreSsl = ignoreSsl;
    }

    public boolean getChunkedMode() {
        return this.chunkedMode;
    }

    public void setChunkedMode(boolean chunkedMode) {
        this.chunkedMode = chunkedMode;
    }

    public boolean getIsSmsEnabled() {
        return this.isSmsEnabled;
    }

    public void setIsSmsEnabled(boolean isSmsEnabled) {
        this.isSmsEnabled = isSmsEnabled;
    }

    public boolean getIsNotificationEnabled() {
        return this.isNotificationEnabled;
    }

    public void setIsNotificationEnabled(boolean isNotificationEnabled) {
        this.isNotificationEnabled = isNotificationEnabled;
    }

    public ActivityType getActivityType() {
        return this.activityType;
    }

    public void setActivityType(ActivityType activityType) {
        this.activityType = activityType;
    }

    // Enhanced data configuration getters and setters
    public boolean isEnhancedDataEnabled() {
        return this.enhancedDataEnabled;
    }

    public void setEnhancedDataEnabled(boolean enhancedDataEnabled) {
        this.enhancedDataEnabled = enhancedDataEnabled;
    }

    public boolean isIncludeDeviceInfo() {
        return this.includeDeviceInfo;
    }

    public void setIncludeDeviceInfo(boolean includeDeviceInfo) {
        this.includeDeviceInfo = includeDeviceInfo;
    }

    public boolean isIncludeSimInfo() {
        return this.includeSimInfo;
    }

    public void setIncludeSimInfo(boolean includeSimInfo) {
        this.includeSimInfo = includeSimInfo;
    }

    public boolean isIncludeNetworkInfo() {
        return this.includeNetworkInfo;
    }

    public void setIncludeNetworkInfo(boolean includeNetworkInfo) {
        this.includeNetworkInfo = includeNetworkInfo;
    }

    public boolean isIncludeAppConfig() {
        return this.includeAppConfig;
    }

    public void setIncludeAppConfig(boolean includeAppConfig) {
        this.includeAppConfig = includeAppConfig;
    }

    public static String getDefaultJsonTemplate() {
        return "{\n  \"from\":\"%from%\",\n  \"text\":\"%text%\",\n  \"sentStamp\":%sentStamp%,\n  \"receivedStamp\":%receivedStamp%,\n  \"sim\":\"%sim%\"\n}";
    }

    public static String getDefaultJsonHeaders() {
        return "{\"User-agent\":\"Android-activity-gateway App\"}";
    }

    public static int getDefaultRetriesNumber() {
        return 10;
    }

    public void save() {
        try {
            if (this.getKey() == null) {
                this.setKey(this.generateKey());
            }

            JSONObject json = new JSONObject();
            json.put(KEY_KEY, this.getKey());
            json.put(KEY_SENDER, this.sender);
            json.put(KEY_URL, this.url);
            json.put(KEY_SIM_SLOT, this.simSlot);
            json.put(KEY_TEMPLATE, this.template);
            json.put(KEY_HEADERS, this.headers);
            json.put(KEY_RETRIES_NUMBER, this.retriesNumber);
            json.put(KEY_IGNORE_SSL, this.ignoreSsl);
            json.put(KEY_CHUNKED_MODE, this.chunkedMode);
            json.put(KEY_IS_SMS_ENABLED, this.isSmsEnabled);
            json.put(KEY_IS_NOTIFICATION_ENABLED, this.isNotificationEnabled);
            json.put(KEY_ACTIVITY_TYPE, this.activityType.getValue());
            json.put("isOn", this.isOn);

            // Enhanced data configuration
            json.put(KEY_ENHANCED_DATA_ENABLED, this.enhancedDataEnabled);
            json.put(KEY_INCLUDE_DEVICE_INFO, this.includeDeviceInfo);
            json.put(KEY_INCLUDE_SIM_INFO, this.includeSimInfo);
            json.put(KEY_INCLUDE_NETWORK_INFO, this.includeNetworkInfo);
            json.put(KEY_INCLUDE_APP_CONFIG, this.includeAppConfig);

            SharedPreferences.Editor editor = getEditor(context);
            editor.putString(this.getKey(), json.toString());

            editor.commit();
        } catch (Exception e) {
            Log.e("ForwardingConfig", e.getMessage());
        }
    }

    public static ArrayList<ForwardingConfig> getAll(Context context) {
        SharedPreferences sharedPref = getPreference(context);
        Map<String, ?> sharedPrefs = sharedPref.getAll();

        ArrayList<ForwardingConfig> configs = new ArrayList<>();

        for (Map.Entry<String, ?> entry : sharedPrefs.entrySet()) {
            ForwardingConfig config = new ForwardingConfig(context);
            config.setSender(entry.getKey());

            String value = (String) entry.getValue();

            if (value.charAt(0) == '{') {
                try {
                    JSONObject json = new JSONObject(value);

                    if (!json.has(KEY_KEY)) {
                        config.setKey(entry.getKey());
                    } else {
                        config.setKey(json.getString(KEY_KEY));
                    }

                    if (!json.has(KEY_SENDER)) {
                        config.setSender(entry.getKey());
                    } else {
                        config.setSender(json.getString(KEY_SENDER));
                    }

                    if (!json.has(KEY_IS_SMS_ENABLED)) {
                        config.setIsSmsEnabled(true);
                    } else {
                        config.setIsSmsEnabled(json.getBoolean(KEY_IS_SMS_ENABLED));
                    }

                    if (json.has("isOn")) {
                        config.isOn = json.getBoolean("isOn");
                    }

                    if (json.has(KEY_SIM_SLOT)) {
                        config.setSimSlot(json.getInt(KEY_SIM_SLOT));
                    }

                    config.setUrl(json.getString(KEY_URL));
                    config.setTemplate(json.getString(KEY_TEMPLATE));
                    config.setHeaders(json.getString(KEY_HEADERS));

                    if (!json.has(KEY_RETRIES_NUMBER)) {
                        config.setRetriesNumber(ForwardingConfig.getDefaultRetriesNumber());
                    } else {
                        config.setRetriesNumber(json.getInt(KEY_RETRIES_NUMBER));
                    }

                    try {
                        config.setIgnoreSsl(json.getBoolean(KEY_IGNORE_SSL));
                        config.setChunkedMode(json.getBoolean(KEY_CHUNKED_MODE));
                    } catch (JSONException ignored) {
                    }

                    if (!json.has(KEY_IS_NOTIFICATION_ENABLED)) {
                        config.setIsNotificationEnabled(false);
                    } else {
                        config.setIsNotificationEnabled(json.getBoolean(KEY_IS_NOTIFICATION_ENABLED));
                    }

                    if (json.has(KEY_ACTIVITY_TYPE)) {
                        config.activityType = ActivityType.fromString(json.getString(KEY_ACTIVITY_TYPE));
                    }

                    // Load enhanced data configuration
                    if (json.has(KEY_ENHANCED_DATA_ENABLED)) {
                        config.enhancedDataEnabled = json.getBoolean(KEY_ENHANCED_DATA_ENABLED);
                    }
                    if (json.has(KEY_INCLUDE_DEVICE_INFO)) {
                        config.includeDeviceInfo = json.getBoolean(KEY_INCLUDE_DEVICE_INFO);
                    }
                    if (json.has(KEY_INCLUDE_SIM_INFO)) {
                        config.includeSimInfo = json.getBoolean(KEY_INCLUDE_SIM_INFO);
                    }
                    if (json.has(KEY_INCLUDE_NETWORK_INFO)) {
                        config.includeNetworkInfo = json.getBoolean(KEY_INCLUDE_NETWORK_INFO);
                    }
                    if (json.has(KEY_INCLUDE_APP_CONFIG)) {
                        config.includeAppConfig = json.getBoolean(KEY_INCLUDE_APP_CONFIG);
                    }

                    config.id = config.getKey().hashCode();
                } catch (JSONException e) {
                    Log.e("ForwardingConfig", e.getMessage());
                }
            } else {
                config.setUrl(value);
                config.setTemplate(ForwardingConfig.getDefaultJsonTemplate());
                config.setHeaders(ForwardingConfig.getDefaultJsonHeaders());
                config.id = config.getKey() != null ? config.getKey().hashCode() : config.sender.hashCode();
            }

            configs.add(config);
        }

        return configs;
    }

    public void remove() {
        SharedPreferences.Editor editor = getEditor(context);
        editor.remove(this.getKey());
        editor.commit();
    }

    public String prepareMessage(String from, String text, String sim, long timeStamp) {
        String template = this.getJsonTemplate();

        template = template.replace("%from%", from);
        template = template.replace("%text%", text);
        template = template.replace("%sim%", sim);
        template = template.replace("%sentStamp%", String.valueOf(timeStamp));
        template = template.replace("%receivedStamp%", String.valueOf(System.currentTimeMillis()));

        return template;
    }

    /**
     * Enhanced SMS message preparation with optional device information
     */
    public String prepareEnhancedMessage(String from, String text, String sim, long timeStamp) {
        // Check if enhanced data is enabled for this specific rule
        if (this.enhancedDataEnabled) {
            try {
                // Create enhanced payload using WebhookSender
                WebhookPayload payload = new WebhookPayload();
                payload.event = "sms_received";
                payload.timestamp = timeStamp;
                payload.deviceId = android.os.Build.MODEL;
                payload.message = "SMS received from " + from;

                // Add SMS-specific data
                payload.addData("from", from);
                payload.addData("text", text);
                payload.addData("sim", sim);
                payload.addData("sentStamp", timeStamp);
                payload.addData("receivedStamp", System.currentTimeMillis());

                // Add enhanced device information based on rule configuration
                addEnhancedDeviceInfoForRule(payload);

                return payload.toJson().toString();
            } catch (Exception e) {
                Log.e("ForwardingConfig", "Error creating enhanced SMS payload, falling back to template", e);
            }
        }

        // Fallback to regular template
        return prepareMessage(from, text, sim, timeStamp);
    }

    public String prepareNotificationMessage(String packageName, String title, String content, String fullMessage,
            long timeStamp) {
        String template = this.getJsonTemplate();

        // Replace notification-specific template variables
        template = template.replace("%from%", packageName);
        template = template.replace("%text%", fullMessage);
        template = template.replace("%title%", title != null ? title : "");
        template = template.replace("%content%", content != null ? content : "");
        template = template.replace("%package%", packageName);
        template = template.replace("%sentStamp%", String.valueOf(timeStamp));
        template = template.replace("%receivedStamp%", String.valueOf(System.currentTimeMillis()));
        template = template.replace("%sim%", "notification"); // For notifications, sim is always "notification"

        return template;
    }

    /**
     * Enhanced notification message preparation with optional device information
     */
    public String prepareEnhancedNotificationMessage(String packageName, String title, String content,
            String fullMessage, long timeStamp) {
        // Check if enhanced data is enabled for this specific rule
        if (this.enhancedDataEnabled) {
            try {
                // Create enhanced payload using WebhookSender
                WebhookPayload payload = new WebhookPayload();
                payload.event = "push_notification_received";
                payload.timestamp = timeStamp;
                payload.deviceId = android.os.Build.MODEL;
                payload.message = "Push notification received from " + packageName;

                // Add notification-specific data
                payload.addData("from", packageName);
                payload.addData("package", packageName);
                payload.addData("title", title != null ? title : "");
                payload.addData("content", content != null ? content : "");
                payload.addData("text", fullMessage);
                payload.addData("sentStamp", timeStamp);
                payload.addData("receivedStamp", System.currentTimeMillis());
                payload.addData("sim", "notification");

                // Add enhanced device information based on rule configuration
                addEnhancedDeviceInfoForRule(payload);

                return payload.toJson().toString();
            } catch (Exception e) {
                Log.e("ForwardingConfig", "Error creating enhanced notification payload, falling back to template", e);
            }
        }

        // Fallback to regular template
        return prepareNotificationMessage(packageName, title, content, fullMessage, timeStamp);
    }

    public String prepareCallMessage(String phoneNumber, String contactName, long timeStamp) {
        String template = this.getJsonTemplate();

        // Replace call-specific template variables
        template = template.replace("%from%", phoneNumber);
        template = template.replace("%contact%", contactName != null ? contactName : "Unknown");
        template = template.replace("%timestamp%", String.valueOf(timeStamp));
        template = template.replace("%duration%", "0"); // Duration is 0 for incoming calls
        template = template.replace("%sentStamp%", String.valueOf(timeStamp));
        template = template.replace("%receivedStamp%", String.valueOf(System.currentTimeMillis()));

        return template;
    }

    // New overloaded method with SIM name
    public String prepareCallMessage(String phoneNumber, String contactName, String simName, long timeStamp) {
        String template = this.getJsonTemplate();

        // Replace call-specific template variables
        template = template.replace("%from%", phoneNumber);
        template = template.replace("%contact%", contactName != null ? contactName : "Unknown");
        template = template.replace("%timestamp%", String.valueOf(timeStamp));
        template = template.replace("%duration%", "0"); // Duration is 0 for incoming calls
        template = template.replace("%sentStamp%", String.valueOf(timeStamp));
        template = template.replace("%receivedStamp%", String.valueOf(System.currentTimeMillis()));
        template = template.replace("%sim%", simName != null ? simName : "undetected");

        return template;
    }

    /**
     * Enhanced call message preparation with optional device information
     */
    public String prepareEnhancedCallMessage(String phoneNumber, String contactName, String simName, long timeStamp) {
        // Check if enhanced data is enabled for this specific rule
        if (this.enhancedDataEnabled) {
            try {
                // Create enhanced payload using WebhookSender
                WebhookPayload payload = new WebhookPayload();
                payload.event = "call_received";
                payload.timestamp = timeStamp;
                payload.deviceId = android.os.Build.MODEL;
                payload.message = "Incoming call from " + phoneNumber;

                // Add call-specific data
                payload.addData("from", phoneNumber);
                payload.addData("contact", contactName != null ? contactName : "Unknown");
                payload.addData("timestamp", timeStamp);
                payload.addData("duration", 0); // Duration is 0 for incoming calls
                payload.addData("sentStamp", timeStamp);
                payload.addData("receivedStamp", System.currentTimeMillis());
                payload.addData("sim", simName != null ? simName : "undetected");

                // Add enhanced device information based on rule configuration
                addEnhancedDeviceInfoForRule(payload);

                return payload.toJson().toString();
            } catch (Exception e) {
                Log.e("ForwardingConfig", "Error creating enhanced call payload, falling back to template", e);
            }
        }

        // Fallback to regular template
        return prepareCallMessage(phoneNumber, contactName, simName, timeStamp);
    }

    /**
     * Add enhanced device information to payload based on this rule's preferences
     */
    private void addEnhancedDeviceInfoForRule(WebhookPayload payload) {
        try {
            JSONObject deviceInfo = DeviceInfoCollector.collectDeviceInfo(context);

            // Filter device info based on this rule's preferences
            JSONObject filteredDeviceInfo = new JSONObject();

            if (this.includeDeviceInfo) {
                // Add basic device information
                if (deviceInfo.has("device_model"))
                    filteredDeviceInfo.put("device_model", deviceInfo.get("device_model"));
                if (deviceInfo.has("device_manufacturer"))
                    filteredDeviceInfo.put("device_manufacturer", deviceInfo.get("device_manufacturer"));
                if (deviceInfo.has("device_brand"))
                    filteredDeviceInfo.put("device_brand", deviceInfo.get("device_brand"));
                if (deviceInfo.has("device_product"))
                    filteredDeviceInfo.put("device_product", deviceInfo.get("device_product"));
                if (deviceInfo.has("android_version"))
                    filteredDeviceInfo.put("android_version", deviceInfo.get("android_version"));
                if (deviceInfo.has("android_sdk"))
                    filteredDeviceInfo.put("android_sdk", deviceInfo.get("android_sdk"));
                if (deviceInfo.has("device_name"))
                    filteredDeviceInfo.put("device_name", deviceInfo.get("device_name"));
            }

            if (this.includeSimInfo && deviceInfo.has("sim_info")) {
                filteredDeviceInfo.put("sim_info", deviceInfo.get("sim_info"));
            }

            if (this.includeNetworkInfo && deviceInfo.has("network_info")) {
                filteredDeviceInfo.put("network_info", deviceInfo.get("network_info"));
            }

            if (this.includeAppConfig && deviceInfo.has("app_config")) {
                filteredDeviceInfo.put("app_config", deviceInfo.get("app_config"));
            }

            // Only add device_info if there's something to include
            if (filteredDeviceInfo.length() > 0) {
                payload.addData("device_info", filteredDeviceInfo);
            }

        } catch (Exception e) {
            Log.e("ForwardingConfig", "Error adding enhanced device info for rule", e);
        }
    }

    private static SharedPreferences getPreference(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }

        try {
            return context.getSharedPreferences(
                    context.getString(R.string.key_phones_preference),
                    Context.MODE_PRIVATE);
        } catch (Exception e) {
            // Fallback to hardcoded preference name if string resource fails
            Log.w("ForwardingConfig", "Failed to get string resource, using fallback", e);
            return context.getSharedPreferences("phones", Context.MODE_PRIVATE);
        }
    }

    private static SharedPreferences.Editor getEditor(Context context) {
        SharedPreferences sharedPref = getPreference(context);
        return sharedPref.edit();
    }

    private String generateKey() {
        String stamp = Long.toString(System.currentTimeMillis());
        int randomNum = new Random().nextInt((999990 - 100000) + 1) + 100000;
        return stamp + '_' + randomNum;
    }

    public String getJsonTemplate() {
        return this.template;
    }

    public void setJsonTemplate(String template) {
        this.template = template;
    }

    public void delete(Context context) {
        remove();
    }

    public void update(Context context) {
        save();
    }
}
