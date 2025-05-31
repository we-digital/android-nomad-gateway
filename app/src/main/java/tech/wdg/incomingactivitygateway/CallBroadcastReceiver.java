package tech.wdg.incomingactivitygateway;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.telephony.TelephonyManager;
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

public class CallBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "CallBroadcastReceiver";
    private Context context;

    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;

        String action = intent.getAction();
        if (action == null) {
            return;
        }

        if (TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(action)) {
            String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            String phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);

            if (TelephonyManager.EXTRA_STATE_RINGING.equals(state) && phoneNumber != null) {
                handleIncomingCall(phoneNumber);
            }
        }
    }

    private void handleIncomingCall(String phoneNumber) {
        Log.d(TAG, "Incoming call from: " + phoneNumber);

        ArrayList<ForwardingConfig> configs = ForwardingConfig.getAll(context);
        String asterisk = context.getString(R.string.asterisk);

        for (ForwardingConfig config : configs) {
            if (!config.isOn) {
                continue;
            }

            // Skip if this is not a CALL rule
            if (config.getActivityType() != ForwardingConfig.ActivityType.CALL) {
                continue;
            }

            // Check if phone number matches
            String configSender = config.getSender();
            boolean phoneMatches = configSender.equals(asterisk) || isPhoneNumberMatch(phoneNumber, configSender);

            if (!phoneMatches) {
                continue;
            }

            Log.d(TAG, "Forwarding call from " + phoneNumber + " via rule: " + config.getKey());

            // Get contact name if available
            String contactName = getContactName(phoneNumber);

            // Send call webhook
            sendCallWebhook(config, phoneNumber, contactName);
        }
    }

    private boolean isPhoneNumberMatch(String incomingNumber, String configNumbers) {
        if (TextUtils.isEmpty(configNumbers) || TextUtils.isEmpty(incomingNumber)) {
            return false;
        }

        // Clean incoming number (remove non-digits)
        String cleanIncoming = incomingNumber.replaceAll("[^0-9]", "");

        // Split config numbers by comma and check each one
        String[] phoneNumbers = configNumbers.split(",");
        for (String phoneNumber : phoneNumbers) {
            String cleanConfig = phoneNumber.trim().replaceAll("[^0-9]", "");

            // Match if the numbers are the same or if one ends with the other (for
            // international vs local)
            if (cleanIncoming.equals(cleanConfig) ||
                    cleanIncoming.endsWith(cleanConfig) ||
                    cleanConfig.endsWith(cleanIncoming)) {
                return true;
            }
        }
        return false;
    }

    private String getContactName(String phoneNumber) {
        try {
            String[] projection = { ContactsContract.PhoneLookup.DISPLAY_NAME };
            String selection = ContactsContract.PhoneLookup.NUMBER + " = ?";
            String[] selectionArgs = { phoneNumber };

            Cursor cursor = context.getContentResolver().query(
                    ContactsContract.PhoneLookup.CONTENT_FILTER_URI.buildUpon()
                            .appendPath(phoneNumber).build(),
                    projection, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                String name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME));
                cursor.close();
                return name;
            }
            if (cursor != null) {
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting contact name: " + e.getMessage());
        }
        return null; // No contact found
    }

    private void sendCallWebhook(ForwardingConfig config, String phoneNumber, String contactName) {
        Data inputData = new Data.Builder()
                .putString("config_key", config.getKey())
                .putString("phone_number", phoneNumber)
                .putString("contact_name", contactName != null ? contactName : "")
                .putLong("timestamp", System.currentTimeMillis())
                .build();

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        WorkRequest workRequest = new OneTimeWorkRequest.Builder(CallWebhookWorker.class)
                .setInputData(inputData)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build();

        WorkManager.getInstance(context).enqueue(workRequest);
    }
}