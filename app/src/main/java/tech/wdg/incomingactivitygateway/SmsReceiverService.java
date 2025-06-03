package tech.wdg.incomingactivitygateway;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.provider.Telephony;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class SmsReceiverService extends Service {

    private static final String TAG = "SmsReceiverService";
    private static final String CHANNEL_ID = "SmsGatewayService";
    private static final String CHANNEL_NAME = "SMS Gateway Service";
    private static final int NOTIFICATION_ID = 1001;
    private static final String PREFS_NAME = "service_state";
    private static final String KEY_SERVICE_RUNNING = "service_running";
    private static final String KEY_START_COUNT = "start_count";

    private BroadcastReceiver receiver;
    private SharedPreferences servicePrefs;
    private NotificationManager notificationManager;

    public SmsReceiverService() {
        receiver = new SmsBroadcastReceiver();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service onCreate");

        // Initialize shared preferences for state tracking
        servicePrefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        notificationManager = getSystemService(NotificationManager.class);

        // Create notification channel for Android 8.0+
        createNotificationChannel();

        // Register SMS receiver
        registerSmsReceiver();

        // Start foreground service with enhanced notification
        startForegroundWithNotification();

        // Update service state
        updateServiceState(true);

        // Trigger auto-start webhook
        triggerAutoStartWebhook();

        Log.d(TAG, "Service created and started in foreground");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service onStartCommand - flags: " + flags + ", startId: " + startId);

        // Increment start count for monitoring
        int startCount = servicePrefs.getInt(KEY_START_COUNT, 0) + 1;
        servicePrefs.edit().putInt(KEY_START_COUNT, startCount).apply();

        // Ensure foreground notification is active
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundWithNotification();
        }

        // Update service state
        updateServiceState(true);

        // Return START_STICKY to ensure service restarts after being killed
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Service onDestroy");
        super.onDestroy();

        // Unregister receiver safely
        try {
            if (receiver != null) {
                unregisterReceiver(receiver);
                Log.d(TAG, "SMS receiver unregistered");
            }
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Receiver was not registered: " + e.getMessage());
        }

        // Stop foreground service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            stopForeground(STOP_FOREGROUND_REMOVE);
        }

        // Update service state
        updateServiceState(false);

        Log.d(TAG, "Service destroyed");
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.d(TAG, "Task removed - attempting to restart service");
        super.onTaskRemoved(rootIntent);

        // Restart service when task is removed (swipe away from recent apps)
        Intent restartServiceIntent = new Intent(getApplicationContext(), this.getClass());
        restartServiceIntent.setPackage(getPackageName());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getApplicationContext().startForegroundService(restartServiceIntent);
        } else {
            getApplicationContext().startService(restartServiceIntent);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Creates notification channel for Android 8.0+ with proper configuration
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && notificationManager != null) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW // Low importance to minimize user distraction
            );

            channel.setDescription("Keeps SMS Gateway running in background to monitor messages");
            channel.setShowBadge(false);
            channel.setSound(null, null);
            channel.enableVibration(false);
            channel.enableLights(false);

            notificationManager.createNotificationChannel(channel);
            Log.d(TAG, "Notification channel created");
        }
    }

    /**
     * Registers SMS broadcast receiver with proper flags for different Android
     * versions
     */
    private void registerSmsReceiver() {
        try {
            IntentFilter filter = new IntentFilter();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                filter.addAction(Telephony.Sms.Intents.SMS_RECEIVED_ACTION);
            } else {
                filter.addAction("android.provider.Telephony.SMS_RECEIVED");
            }

            // Register receiver with appropriate flags for Android 14+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(receiver, filter, RECEIVER_EXPORTED);
            } else {
                registerReceiver(receiver, filter);
            }

            Log.d(TAG, "SMS receiver registered successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to register SMS receiver", e);
        }
    }

    /**
     * Starts foreground service with enhanced notification
     */
    private void startForegroundWithNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                Notification notification = createServiceNotification();
                startForeground(NOTIFICATION_ID, notification);
                Log.d(TAG, "Started foreground service with notification");
            } catch (SecurityException e) {
                Log.e(TAG, "Failed to start foreground service - permission denied", e);
                // Try to handle the permission issue gracefully
                handleForegroundServicePermissionDenied();
            } catch (Exception e) {
                Log.e(TAG, "Failed to start foreground service", e);
            }
        }
    }

    /**
     * Handles foreground service permission denied scenario
     */
    private void handleForegroundServicePermissionDenied() {
        Log.w(TAG, "Foreground service permission denied - service may be killed by system");

        // Update service state to indicate permission issue
        servicePrefs.edit()
                .putBoolean(KEY_SERVICE_RUNNING, false)
                .putString("permission_error", "FOREGROUND_SERVICE_DENIED")
                .putLong("last_error", System.currentTimeMillis())
                .apply();

        // Try to restart the service after a delay
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            Log.d(TAG, "Attempting to restart service after permission error");
            ServiceRestartReceiver.triggerServiceRestart(getApplicationContext());
        }, 5000); // 5 second delay
    }

    /**
     * Creates enhanced notification for foreground service
     */
    private Notification createServiceNotification() {
        // Create intent to open main activity when notification is tapped
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Get service statistics
        int startCount = servicePrefs.getInt(KEY_START_COUNT, 0);
        String statusText = "Active • Started " + startCount + " times";

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("SMS Gateway Active")
                .setContentText(statusText)
                .setSmallIcon(R.drawable.ic_f)
                .setColor(getColor(R.color.colorPrimary))
                .setContentIntent(pendingIntent)
                .setOngoing(true) // Cannot be dismissed by user
                .setAutoCancel(false)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setShowWhen(false)
                .build();
    }

    /**
     * Updates service state in shared preferences for monitoring
     */
    private void updateServiceState(boolean isRunning) {
        servicePrefs.edit()
                .putBoolean(KEY_SERVICE_RUNNING, isRunning)
                .putLong("last_update", System.currentTimeMillis())
                .apply();

        Log.d(TAG, "Service state updated: running=" + isRunning);
    }

    /**
     * Static method to check if service should be running
     */
    public static boolean isServiceExpectedToRun(android.content.Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getBoolean(KEY_SERVICE_RUNNING, false);
    }

    /**
     * Static method to get service statistics
     */
    public static int getServiceStartCount(android.content.Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getInt(KEY_START_COUNT, 0);
    }

    /**
     * Triggers webhook for auto-start event
     */
    private void triggerAutoStartWebhook() {
        // Check if auto-start webhook is enabled
        if (AppWebhooksActivity.isAutoStartWebhookEnabled(this)) {
            String url = AppWebhooksActivity.getAutoStartWebhookUrl(this);
            if (!url.isEmpty()) {
                WebhookPayload payload = WebhookSender.createAppStartPayload(this, false);
                WebhookSender.sendWebhook(this, url, payload);
                Log.d(TAG, "Auto-start webhook triggered");
            }
        }
    }
}