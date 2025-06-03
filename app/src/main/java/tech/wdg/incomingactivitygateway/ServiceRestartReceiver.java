package tech.wdg.incomingactivitygateway;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

/**
 * Broadcast receiver to handle service restart scenarios
 * This helps ensure the service stays running even if killed by the system
 */
public class ServiceRestartReceiver extends BroadcastReceiver {

    private static final String TAG = "ServiceRestartReceiver";
    public static final String ACTION_RESTART_SERVICE = "tech.wdg.incomingactivitygateway.RESTART_SERVICE";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "Received broadcast: " + action);

        if (ACTION_RESTART_SERVICE.equals(action)) {
            restartService(context);
        } else if (Intent.ACTION_MY_PACKAGE_REPLACED.equals(action) ||
                Intent.ACTION_PACKAGE_REPLACED.equals(action)) {
            // App was updated, restart service
            Log.d(TAG, "App updated - restarting service");
            restartService(context);
        }
    }

    private void restartService(Context context) {
        try {
            // Only restart if service was expected to be running
            if (SmsReceiverService.isServiceExpectedToRun(context)) {
                Log.d(TAG, "Restarting SMS Gateway service");

                Intent serviceIntent = new Intent(context, SmsReceiverService.class);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent);
                } else {
                    context.startService(serviceIntent);
                }

                Log.d(TAG, "Service restart requested");
            } else {
                Log.d(TAG, "Service not expected to run - skipping restart");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to restart service", e);
        }
    }

    /**
     * Static method to trigger service restart
     */
    public static void triggerServiceRestart(Context context) {
        Intent intent = new Intent(context, ServiceRestartReceiver.class);
        intent.setAction(ACTION_RESTART_SERVICE);
        context.sendBroadcast(intent);
    }
}