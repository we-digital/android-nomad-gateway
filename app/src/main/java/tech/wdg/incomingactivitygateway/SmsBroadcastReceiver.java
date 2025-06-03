package tech.wdg.incomingactivitygateway;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;

import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class SmsBroadcastReceiver extends BroadcastReceiver {

    private Context context;

    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;

        Bundle bundle = intent.getExtras();
        if (bundle == null) {
            return;
        }

        Object[] pdus = (Object[]) bundle.get("pdus");
        String format = bundle.getString("format");
        if (pdus == null || pdus.length == 0) {
            return;
        }

        StringBuilder content = new StringBuilder();
        final SmsMessage[] messages = new SmsMessage[pdus.length];
        for (int i = 0; i < pdus.length; i++) {
            if (format != null) {
                messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i], format);
            } else {
                messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
            }
            content.append(messages[i].getDisplayMessageBody());
        }

        ArrayList<ForwardingConfig> configs = ForwardingConfig.getAll(context);
        String asterisk = context.getString(R.string.asterisk);

        String sender = messages[0].getOriginatingAddress();
        if (sender == null) {
            return;
        }

        for (ForwardingConfig config : configs) {
            if (!config.isOn) {
                continue;
            }

            // Skip if this is not an SMS rule
            if (config.getActivityType() != ForwardingConfig.ActivityType.SMS) {
                continue;
            }

            // Check if sender matches (now supports comma-separated numbers)
            String configSender = config.getSender();
            boolean senderMatches = configSender.equals(asterisk) || isPhoneNumberMatch(sender, configSender);

            if (!senderMatches) {
                continue;
            }

            int slotId = this.detectSim(bundle) + 1;
            String slotName = "undetected";
            if (slotId < 0) {
                slotId = 0;
            }

            if (config.getSimSlot() > 0 && config.getSimSlot() != slotId) {
                continue;
            }

            if (slotId > 0) {
                // Use the new operator settings to get SIM name
                slotName = OperatorSettingsActivity.getSimName(context, slotId - 1);
            }

            this.callWebHook(config, sender, slotName, content.toString(), messages[0].getTimestampMillis());
        }
    }

    protected void callWebHook(ForwardingConfig config, String sender, String slotName,
            String content, long timeStamp) {

        // Use enhanced message preparation if enabled, otherwise use regular template
        String message = config.prepareEnhancedMessage(sender, content, slotName, timeStamp);

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

    private int detectSim(Bundle bundle) {
        int slotId = -1;
        Set<String> keySet = bundle.keySet();
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
                    if (key.toLowerCase().contains("slot") | key.toLowerCase().contains("sim")) {
                        String value = bundle.getString(key, "-1");
                        if (value.equals("0") | value.equals("1") | value.equals("2")) {
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

    private boolean isPhoneNumberMatch(String incomingNumber, String configNumbers) {
        if (android.text.TextUtils.isEmpty(configNumbers) || android.text.TextUtils.isEmpty(incomingNumber)) {
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
}
