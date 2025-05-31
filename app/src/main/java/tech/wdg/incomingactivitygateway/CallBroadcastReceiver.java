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
import android.os.Build;
import android.os.Bundle;

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
            Bundle bundle = intent.getExtras();

            // Handle deprecated EXTRA_INCOMING_NUMBER
            String phoneNumber = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                // For Android 10+, incoming number is restricted for privacy
                // We need to use CallLog to get the number after the call
                if (TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
                    // For ringing state, we can try to get the most recent call log entry
                    phoneNumber = getLatestIncomingNumber();
                }
            } else {
                // For older versions, use the deprecated method
                phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
            }

            if (TelephonyManager.EXTRA_STATE_RINGING.equals(state) && phoneNumber != null) {
                // Detect SIM slot
                int slotId = detectSim(bundle);
                handleIncomingCall(phoneNumber, slotId);
            }
        }
    }

    private void handleIncomingCall(String phoneNumber, int slotId) {
        Log.d(TAG, "Incoming call from: " + phoneNumber + " on SIM slot: " + slotId);

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

            // Check SIM slot filtering
            if (config.getSimSlot() > 0 && config.getSimSlot() != (slotId + 1)) {
                continue;
            }

            Log.d(TAG, "Forwarding call from " + phoneNumber + " via rule: " + config.getKey());

            // Get contact name if available
            String contactName = getContactName(phoneNumber);

            // Get SIM name
            String simName = "undetected";
            if (slotId >= 0) {
                simName = OperatorSettingsActivity.getSimName(context, slotId);
            }

            // Send call webhook
            sendCallWebhook(config, phoneNumber, contactName, simName);
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

    private void sendCallWebhook(ForwardingConfig config, String phoneNumber, String contactName, String simName) {
        Data inputData = new Data.Builder()
                .putString("config_key", config.getKey())
                .putString("phone_number", phoneNumber)
                .putString("contact_name", contactName != null ? contactName : "")
                .putString("sim_name", simName)
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

    private String getLatestIncomingNumber() {
        try {
            // Query the call log for the most recent incoming call
            String[] projection = {
                    CallLog.Calls.NUMBER,
                    CallLog.Calls.TYPE,
                    CallLog.Calls.DATE
            };

            String selection = CallLog.Calls.TYPE + " = ?";
            String[] selectionArgs = { String.valueOf(CallLog.Calls.INCOMING_TYPE) };
            String sortOrder = CallLog.Calls.DATE + " DESC LIMIT 1";

            Cursor cursor = context.getContentResolver().query(
                    CallLog.Calls.CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    sortOrder);

            if (cursor != null && cursor.moveToFirst()) {
                String number = cursor.getString(cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER));
                cursor.close();
                return number;
            }

            if (cursor != null) {
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting latest incoming number: " + e.getMessage());
        }
        return null;
    }

    private int detectSim(Bundle bundle) {
        int slotId = -1;
        if (bundle == null) {
            return slotId;
        }

        java.util.Set<String> keySet = bundle.keySet();
        for (String key : keySet) {
            switch (key) {
                case "phone":
                    slotId = bundle.getInt("phone", -1);
                    break;
                case "slot":
                    slotId = bundle.getInt("slot", -1);
                    break;
                case "simId":
                    slotId = bundle.getInt("simId", -1);
                    break;
                case "simSlot":
                    slotId = bundle.getInt("simSlot", -1);
                    break;
                case "slot_id":
                    slotId = bundle.getInt("slot_id", -1);
                    break;
                case "simnum":
                    slotId = bundle.getInt("simnum", -1);
                    break;
                case "slotId":
                    slotId = bundle.getInt("slotId", -1);
                    break;
                case "slotIdx":
                    slotId = bundle.getInt("slotIdx", -1);
                    break;
                case "android.telephony.extra.SLOT_INDEX":
                    slotId = bundle.getInt("android.telephony.extra.SLOT_INDEX", -1);
                    break;
                default:
                    if (key.toLowerCase().contains("slot") || key.toLowerCase().contains("sim")) {
                        String value = bundle.getString(key, "-1");
                        if (value.equals("0") || value.equals("1") || value.equals("2")) {
                            slotId = bundle.getInt(key, -1);
                        }
                    }
            }

            if (slotId != -1) {
                break;
            }
        }

        return slotId;
    }
}