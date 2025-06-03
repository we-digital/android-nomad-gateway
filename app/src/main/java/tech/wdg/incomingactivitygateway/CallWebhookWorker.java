package tech.wdg.incomingactivitygateway;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.ArrayList;

public class CallWebhookWorker extends Worker {

    private static final String TAG = "CallWebhookWorker";

    public CallWebhookWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            String configKey = getInputData().getString("config_key");
            String phoneNumber = getInputData().getString("phone_number");
            String contactName = getInputData().getString("contact_name");
            String simName = getInputData().getString("sim_name");
            long timestamp = getInputData().getLong("timestamp", System.currentTimeMillis());

            if (configKey == null || phoneNumber == null) {
                Log.e(TAG, "Missing required data for call webhook");
                return Result.failure();
            }

            // Find the config
            ForwardingConfig config = findConfigByKey(configKey);
            if (config == null) {
                Log.e(TAG, "Config not found: " + configKey);
                return Result.failure();
            }

            // Use enhanced message preparation if enabled, otherwise use regular template
            String payload = config.prepareEnhancedCallMessage(phoneNumber, contactName, simName, timestamp);

            // Send the webhook
            Request request = new Request(config.getUrl(), payload);
            request.setJsonHeaders(config.getHeaders());
            request.setIgnoreSsl(config.getIgnoreSsl());
            request.setUseChunkedMode(config.getChunkedMode());

            String result = request.execute();

            if (Request.RESULT_SUCCESS.equals(result)) {
                Log.d(TAG, "Call webhook sent successfully for: " + phoneNumber);
                return Result.success();
            } else {
                Log.e(TAG, "Call webhook failed: " + result);
                return Result.retry();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error in call webhook worker", e);
            return Result.failure();
        }
    }

    private ForwardingConfig findConfigByKey(String key) {
        ArrayList<ForwardingConfig> configs = ForwardingConfig.getAll(getApplicationContext());
        for (ForwardingConfig config : configs) {
            if (config.getKey().equals(key)) {
                return config;
            }
        }
        return null;
    }
}