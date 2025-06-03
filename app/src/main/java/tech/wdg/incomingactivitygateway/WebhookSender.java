package tech.wdg.incomingactivitygateway;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.HttpsURLConnection;

/**
 * Utility class for sending webhooks
 */
public class WebhookSender {
    private static final String TAG = "WebhookSender";
    private static final int TIMEOUT_MS = 30000; // 30 seconds
    private static final ExecutorService executor = Executors.newCachedThreadPool();

    public interface WebhookCallback {
        void onSuccess(String response);

        void onError(String error);
    }

    /**
     * Send a webhook asynchronously
     */
    public static void sendWebhook(Context context, String url, WebhookPayload payload, WebhookCallback callback) {
        executor.execute(() -> {
            try {
                String response = sendWebhookSync(context, url, payload);
                if (callback != null) {
                    callback.onSuccess(response);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to send webhook to " + url, e);
                if (callback != null) {
                    callback.onError(e.getMessage());
                }
            }
        });
    }

    /**
     * Send a webhook synchronously
     */
    public static String sendWebhookSync(Context context, String urlString, WebhookPayload payload)
            throws IOException, JSONException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        try {
            // Configure connection
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("User-Agent", "AndroidNomadGateway/1.0");
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);
            connection.setDoOutput(true);

            // Set up SSL if needed
            if (connection instanceof HttpsURLConnection) {
                HttpsURLConnection httpsConnection = (HttpsURLConnection) connection;
                // Use custom SSL factory if available
                if (CustomSSLSocketFactory.getInstance(context) != null) {
                    httpsConnection.setSSLSocketFactory(CustomSSLSocketFactory.getInstance(context));
                }
            }

            // Send the payload
            String jsonPayload = payload.toJson().toString();
            Log.d(TAG, "Sending webhook to " + urlString + ": " + jsonPayload);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // Get response
            int responseCode = connection.getResponseCode();
            Log.d(TAG, "Webhook response code: " + responseCode);

            StringBuilder response = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(
                            responseCode >= 200 && responseCode < 300
                                    ? connection.getInputStream()
                                    : connection.getErrorStream(),
                            StandardCharsets.UTF_8))) {
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
            }

            if (responseCode >= 200 && responseCode < 300) {
                return response.toString();
            } else {
                throw new IOException("HTTP error code: " + responseCode + " - " + response.toString());
            }

        } finally {
            connection.disconnect();
        }
    }

    /**
     * Send a simple webhook without callback
     */
    public static void sendWebhook(Context context, String url, WebhookPayload payload) {
        sendWebhook(context, url, payload, null);
    }

    /**
     * Create a webhook payload for app start events
     */
    public static WebhookPayload createAppStartPayload(Context context, boolean isManualStart) {
        WebhookPayload payload = new WebhookPayload();
        payload.event = isManualStart ? "app_manual_start" : "app_auto_start";
        payload.timestamp = System.currentTimeMillis();
        payload.deviceId = android.os.Build.MODEL;
        payload.message = isManualStart
                ? "Application started manually by user"
                : "Application started automatically";

        try {
            payload.addData("android_version", android.os.Build.VERSION.RELEASE);
            payload.addData("app_version", context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0).versionName);
        } catch (Exception e) {
            Log.e(TAG, "Error adding app info to payload", e);
        }

        return payload;
    }

    /**
     * Create a webhook payload for SIM status changes
     */
    public static WebhookPayload createSimStatusPayload(Context context, String simStatus, String operator) {
        WebhookPayload payload = new WebhookPayload();
        payload.event = "sim_status_changed";
        payload.timestamp = System.currentTimeMillis();
        payload.deviceId = android.os.Build.MODEL;
        payload.message = "SIM status changed to: " + simStatus;

        try {
            payload.addData("sim_status", simStatus);
            payload.addData("operator", operator);
            payload.addData("android_version", android.os.Build.VERSION.RELEASE);
        } catch (JSONException e) {
            Log.e(TAG, "Error adding SIM info to payload", e);
        }

        return payload;
    }
}