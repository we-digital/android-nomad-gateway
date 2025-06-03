package tech.wdg.incomingactivitygateway;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class BootCompletedReceiver extends BroadcastReceiver {

    private static final String TAG = "BootCompletedReceiver";

    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    @Override
    public void onReceive(Context context, Intent argIntent) {
        Log.d(TAG, "Boot completed - starting SMS Gateway service");

        try {
            Intent intent = new Intent(context, SmsReceiverService.class);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent);
                Log.d(TAG, "Foreground service start requested");
            } else {
                context.startService(intent);
                Log.d(TAG, "Service start requested");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to start service on boot", e);
        }
    }
}
