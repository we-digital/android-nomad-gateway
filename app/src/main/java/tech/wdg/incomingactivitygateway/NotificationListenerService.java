package tech.wdg.incomingactivitygateway;

import android.app.Notification;
import android.content.Context;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.Log;

import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class NotificationListenerService extends android.service.notification.NotificationListenerService {

    private static final String TAG = "NotificationListener";
    private Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        this.context = this;
        Log.d(TAG, "NotificationListenerService created");
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);

        try {
            // Get notification details
            Notification notification = sbn.getNotification();
            String packageName = sbn.getPackageName();

            // Skip our own notifications
            if (packageName.equals(getPackageName())) {
                return;
            }

            // Extract notification content
            Bundle extras = notification.extras;
            if (extras == null) {
                return;
            }

            String title = extras.getString(Notification.EXTRA_TITLE, "");
            String text = extras.getString(Notification.EXTRA_TEXT, "");
            String bigText = extras.getString(Notification.EXTRA_BIG_TEXT, "");

            // Use big text if available, otherwise use regular text
            String content = !TextUtils.isEmpty(bigText) ? bigText : text;

            // Skip if no meaningful content
            if (TextUtils.isEmpty(title) && TextUtils.isEmpty(content)) {
                return;
            }

            // Create notification message
            String notificationMessage = buildNotificationMessage(packageName, title, content);

            Log.d(TAG, "Processing notification from " + packageName + ": " + notificationMessage);

            // Process forwarding rules
            processNotificationForwarding(packageName, title, content, notificationMessage);

        } catch (Exception e) {
            Log.e(TAG, "Error processing notification", e);
        }
    }

    private String buildNotificationMessage(String packageName, String title, String content) {
        StringBuilder message = new StringBuilder();

        if (!TextUtils.isEmpty(title)) {
            message.append(title);
        }

        if (!TextUtils.isEmpty(content)) {
            if (message.length() > 0) {
                message.append(": ");
            }
            message.append(content);
        }

        return message.toString();
    }

    private void processNotificationForwarding(String packageName, String title, String content, String fullMessage) {
        ArrayList<ForwardingConfig> configs = ForwardingConfig.getAll(context);
        String asterisk = context.getString(R.string.asterisk);

        for (ForwardingConfig config : configs) {
            // Skip if rule is disabled
            if (!config.isOn) {
                continue;
            }

            // Skip if this is not a PUSH notification rule
            if (config.getActivityType() != ForwardingConfig.ActivityType.PUSH) {
                continue;
            }

            // Check if sender matches (for notifications, sender is the app package name)
            String configSender = config.getSender();
            boolean senderMatches = configSender.equals(asterisk) ||
                    configSender.equals(packageName) ||
                    packageName.contains(configSender);

            if (!senderMatches) {
                continue;
            }

            Log.d(TAG, "Forwarding notification from " + packageName + " via rule: " + config.getKey());

            // Prepare and send the notification
            sendNotificationWebhook(config, packageName, title, content, fullMessage);
        }
    }

    private void sendNotificationWebhook(ForwardingConfig config, String packageName, String title, String content,
            String fullMessage) {
        long timeStamp = System.currentTimeMillis();

        // Use enhanced message preparation if enabled, otherwise use regular template
        String message = config.prepareEnhancedNotificationMessage(packageName, title, content, fullMessage, timeStamp);

        // Create work request for sending webhook
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        Data data = new Data.Builder()
                .putString(RequestWorker.DATA_URL, config.getUrl())
                .putString(RequestWorker.DATA_TEXT, message)
                .putString(RequestWorker.DATA_HEADERS, config.getHeaders())
                .putBoolean(RequestWorker.DATA_IGNORE_SSL, config.getIgnoreSsl())
                .putBoolean(RequestWorker.DATA_CHUNKED_MODE, config.getChunkedMode())
                .putInt(RequestWorker.DATA_MAX_RETRIES, config.getRetriesNumber())
                .build();

        WorkRequest workRequest = new OneTimeWorkRequest.Builder(RequestWorker.class)
                .setConstraints(constraints)
                .setBackoffCriteria(
                        BackoffPolicy.EXPONENTIAL,
                        OneTimeWorkRequest.MIN_BACKOFF_MILLIS,
                        TimeUnit.MILLISECONDS)
                .setInputData(data)
                .build();

        WorkManager
                .getInstance(this.context)
                .enqueue(workRequest);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        super.onNotificationRemoved(sbn);
        // We don't need to handle notification removal for forwarding
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "NotificationListenerService destroyed");
    }
}