package tech.wdg.incomingactivitygateway;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.util.Log;

/**
 * Broadcast receiver to monitor SIM card status changes
 */
public class SimStatusReceiver extends BroadcastReceiver {
    private static final String TAG = "SimStatusReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }

        String action = intent.getAction();
        Log.d(TAG, "Received action: " + action);

        // Check for SIM state changed action
        if ("android.intent.action.SIM_STATE_CHANGED".equals(action)) {
            handleSimStateChanged(context, intent);
        }
    }

    private void handleSimStateChanged(Context context, Intent intent) {
        String simState = intent.getStringExtra("ss");
        Log.d(TAG, "SIM state changed to: " + simState);

        // Get telephony manager for additional info
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        String operator = "Unknown";

        if (telephonyManager != null) {
            try {
                // Get operator name
                operator = telephonyManager.getNetworkOperatorName();
                if (operator == null || operator.isEmpty()) {
                    operator = telephonyManager.getSimOperatorName();
                }
                if (operator == null || operator.isEmpty()) {
                    operator = "Unknown";
                }
            } catch (SecurityException e) {
                Log.e(TAG, "Permission denied to read phone state", e);
            }
        }

        // Map SIM states to readable status
        String readableStatus = mapSimState(simState);

        // Trigger webhook if enabled
        triggerSimStatusWebhook(context, readableStatus, operator);
    }

    private String mapSimState(String simState) {
        if (simState == null) {
            return "UNKNOWN";
        }

        switch (simState) {
            case "ABSENT":
                return "SIM_ABSENT";
            case "LOCKED":
                return "SIM_LOCKED";
            case "READY":
                return "SIM_READY";
            case "NOT_READY":
                return "SIM_NOT_READY";
            case "PERM_DISABLED":
                return "SIM_PERMANENTLY_DISABLED";
            case "CARD_IO_ERROR":
                return "SIM_CARD_ERROR";
            case "CARD_RESTRICTED":
                return "SIM_RESTRICTED";
            case "LOADED":
                return "SIM_LOADED";
            case "PRESENT":
                return "SIM_PRESENT";
            default:
                return "SIM_" + simState.toUpperCase();
        }
    }

    private void triggerSimStatusWebhook(Context context, String simStatus, String operator) {
        // Check if SIM status webhook is enabled
        if (AppWebhooksActivity.isSimStatusWebhookEnabled(context)) {
            String url = AppWebhooksActivity.getSimStatusWebhookUrl(context);
            if (!url.isEmpty()) {
                WebhookPayload payload = WebhookSender.createSimStatusPayload(context, simStatus, operator);
                WebhookSender.sendWebhook(context, url, payload);
                Log.d(TAG, "SIM status webhook triggered - Status: " + simStatus + ", Operator: " + operator);
            }
        }
    }
}