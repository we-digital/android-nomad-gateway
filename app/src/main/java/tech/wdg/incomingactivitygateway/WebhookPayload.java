package tech.wdg.incomingactivitygateway;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Data class representing a webhook payload
 */
public class WebhookPayload {
    public String event;
    public long timestamp;
    public String deviceId;
    public String message;
    public JSONObject additionalData;

    public WebhookPayload() {
        this.additionalData = new JSONObject();
    }

    /**
     * Convert the payload to JSON format
     */
    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("event", event);
        json.put("timestamp", timestamp);
        json.put("device_id", deviceId);
        json.put("message", message);

        // Add any additional data
        if (additionalData != null && additionalData.length() > 0) {
            json.put("data", additionalData);
        }

        return json;
    }

    /**
     * Add custom data to the payload
     */
    public void addData(String key, Object value) throws JSONException {
        if (additionalData == null) {
            additionalData = new JSONObject();
        }
        additionalData.put(key, value);
    }
}