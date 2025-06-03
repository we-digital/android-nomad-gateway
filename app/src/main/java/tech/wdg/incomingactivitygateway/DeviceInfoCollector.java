package tech.wdg.incomingactivitygateway;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

/**
 * Utility class to collect device information for webhooks
 * Only uses information available within current app permissions
 */
public class DeviceInfoCollector {
    private static final String TAG = "DeviceInfoCollector";

    /**
     * Collect comprehensive device information
     */
    public static JSONObject collectDeviceInfo(Context context) {
        JSONObject deviceInfo = new JSONObject();

        try {
            // Basic device information
            deviceInfo.put("device_model", Build.MODEL);
            deviceInfo.put("device_manufacturer", Build.MANUFACTURER);
            deviceInfo.put("device_brand", Build.BRAND);
            deviceInfo.put("device_product", Build.PRODUCT);
            deviceInfo.put("android_version", Build.VERSION.RELEASE);
            deviceInfo.put("android_sdk", Build.VERSION.SDK_INT);
            deviceInfo.put("device_name", getDeviceName(context));

            // SIM information
            deviceInfo.put("sim_info", collectSimInfo(context));

            // Network information
            deviceInfo.put("network_info", collectNetworkInfo(context));

            // App configuration
            deviceInfo.put("app_config", collectAppConfig(context));

        } catch (JSONException e) {
            Log.e(TAG, "Error collecting device info", e);
        }

        return deviceInfo;
    }

    /**
     * Get device name from settings
     */
    private static String getDeviceName(Context context) {
        try {
            String deviceName = Settings.Global.getString(context.getContentResolver(), Settings.Global.DEVICE_NAME);
            if (deviceName != null && !deviceName.isEmpty()) {
                return deviceName;
            }

            // Fallback to Bluetooth name
            deviceName = Settings.Secure.getString(context.getContentResolver(), "bluetooth_name");
            if (deviceName != null && !deviceName.isEmpty()) {
                return deviceName;
            }

            // Final fallback to model
            return Build.MODEL;
        } catch (Exception e) {
            Log.w(TAG, "Could not get device name", e);
            return Build.MODEL;
        }
    }

    /**
     * Collect SIM card information
     */
    private static JSONObject collectSimInfo(Context context) {
        JSONObject simInfo = new JSONObject();

        try {
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (telephonyManager == null) {
                return simInfo;
            }

            // Check if we have phone state permission
            boolean hasPhonePermission = ContextCompat.checkSelfPermission(context,
                    Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED;

            if (hasPhonePermission) {
                // Basic SIM state
                int simState = telephonyManager.getSimState();
                simInfo.put("sim_state", mapSimState(simState));

                // Network operator information
                String networkOperator = telephonyManager.getNetworkOperatorName();
                if (networkOperator != null && !networkOperator.isEmpty()) {
                    simInfo.put("network_operator", networkOperator);
                }

                String simOperator = telephonyManager.getSimOperatorName();
                if (simOperator != null && !simOperator.isEmpty()) {
                    simInfo.put("sim_operator", simOperator);
                }

                // Country codes
                String networkCountry = telephonyManager.getNetworkCountryIso();
                if (networkCountry != null && !networkCountry.isEmpty()) {
                    simInfo.put("network_country", networkCountry.toUpperCase());
                }

                String simCountry = telephonyManager.getSimCountryIso();
                if (simCountry != null && !simCountry.isEmpty()) {
                    simInfo.put("sim_country", simCountry.toUpperCase());
                }

                // Phone type
                int phoneType = telephonyManager.getPhoneType();
                simInfo.put("phone_type", mapPhoneType(phoneType));

                // Network type
                int networkType = telephonyManager.getNetworkType();
                simInfo.put("network_type", mapNetworkType(networkType));

                // Multiple SIM support (Android 5.1+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                    simInfo.put("sim_slots", collectMultiSimInfo(context));
                }
            } else {
                simInfo.put("error", "Phone state permission not granted");
            }

        } catch (SecurityException e) {
            Log.w(TAG, "Permission denied for SIM info", e);
            try {
                simInfo.put("error", "Permission denied");
            } catch (JSONException ignored) {
            }
        } catch (Exception e) {
            Log.e(TAG, "Error collecting SIM info", e);
        }

        return simInfo;
    }

    /**
     * Collect multiple SIM information for dual SIM devices
     */
    private static JSONArray collectMultiSimInfo(Context context) {
        JSONArray simSlots = new JSONArray();

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                SubscriptionManager subscriptionManager = SubscriptionManager.from(context);
                List<SubscriptionInfo> subscriptions = subscriptionManager.getActiveSubscriptionInfoList();

                if (subscriptions != null) {
                    for (SubscriptionInfo subscription : subscriptions) {
                        JSONObject slot = new JSONObject();
                        slot.put("slot_index", subscription.getSimSlotIndex());
                        slot.put("subscription_id", subscription.getSubscriptionId());
                        slot.put("display_name", subscription.getDisplayName());
                        slot.put("carrier_name", subscription.getCarrierName());
                        slot.put("country_iso", subscription.getCountryIso());

                        // Phone number (if available and permitted)
                        String phoneNumber = subscription.getNumber();
                        if (phoneNumber != null && !phoneNumber.isEmpty()) {
                            slot.put("phone_number", phoneNumber);
                        }

                        simSlots.put(slot);
                    }
                }
            }
        } catch (SecurityException e) {
            Log.w(TAG, "Permission denied for subscription info", e);
        } catch (Exception e) {
            Log.e(TAG, "Error collecting multi-SIM info", e);
        }

        return simSlots;
    }

    /**
     * Collect network and connectivity information
     */
    private static JSONObject collectNetworkInfo(Context context) {
        JSONObject networkInfo = new JSONObject();

        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager != null) {
                NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
                if (activeNetwork != null) {
                    networkInfo.put("is_connected", activeNetwork.isConnected());
                    networkInfo.put("connection_type", activeNetwork.getTypeName());
                    networkInfo.put("connection_subtype", activeNetwork.getSubtypeName());
                    networkInfo.put("is_roaming", activeNetwork.isRoaming());
                }
            }

            // WiFi information (check permission first)
            boolean hasWifiPermission = ContextCompat.checkSelfPermission(context,
                    Manifest.permission.ACCESS_WIFI_STATE) == PackageManager.PERMISSION_GRANTED;

            if (hasWifiPermission) {
                try {
                    WifiManager wifiManager = (WifiManager) context.getApplicationContext()
                            .getSystemService(Context.WIFI_SERVICE);
                    if (wifiManager != null && wifiManager.isWifiEnabled()) {
                        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                        if (wifiInfo != null) {
                            JSONObject wifi = new JSONObject();
                            wifi.put("ssid", wifiInfo.getSSID());
                            wifi.put("bssid", wifiInfo.getBSSID());
                            wifi.put("rssi", wifiInfo.getRssi());
                            wifi.put("link_speed", wifiInfo.getLinkSpeed());
                            wifi.put("frequency", wifiInfo.getFrequency());
                            networkInfo.put("wifi_info", wifi);
                        }
                    }
                } catch (SecurityException e) {
                    Log.w(TAG, "WiFi permission denied despite check", e);
                    try {
                        networkInfo.put("wifi_error", "Permission denied");
                    } catch (JSONException ignored) {
                    }
                }
            } else {
                try {
                    networkInfo.put("wifi_error", "WiFi access permission not granted");
                } catch (JSONException ignored) {
                }
            }

            // IP address information
            networkInfo.put("ip_addresses", collectIpAddresses());

        } catch (Exception e) {
            Log.e(TAG, "Error collecting network info", e);
        }

        return networkInfo;
    }

    /**
     * Collect IP address information
     */
    private static JSONObject collectIpAddresses() {
        JSONObject ipInfo = new JSONObject();

        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            JSONArray addresses = new JSONArray();

            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        JSONObject addressInfo = new JSONObject();
                        String hostAddress = addr.getHostAddress();

                        if (hostAddress != null) {
                            addressInfo.put("address", hostAddress);
                            addressInfo.put("interface", intf.getName());
                            addressInfo.put("is_ipv4", hostAddress.indexOf(':') < 0);
                            addressInfo.put("is_site_local", addr.isSiteLocalAddress());
                            addresses.put(addressInfo);
                        }
                    }
                }
            }

            ipInfo.put("all_addresses", addresses);

            // Try to get primary IP
            String primaryIp = getPrimaryIpAddress();
            if (primaryIp != null) {
                ipInfo.put("primary_ip", primaryIp);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error collecting IP addresses", e);
        }

        return ipInfo;
    }

    /**
     * Get primary IP address
     */
    private static String getPrimaryIpAddress() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String hostAddress = addr.getHostAddress();
                        if (hostAddress != null && hostAddress.indexOf(':') < 0) { // IPv4
                            return hostAddress;
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting primary IP", e);
        }
        return null;
    }

    /**
     * Collect app configuration information
     */
    private static JSONObject collectAppConfig(Context context) {
        JSONObject appConfig = new JSONObject();

        try {
            // App version info
            String versionName = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0).versionName;
            int versionCode = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0).versionCode;

            appConfig.put("version_name", versionName);
            appConfig.put("version_code", versionCode);
            appConfig.put("package_name", context.getPackageName());

            // Service status
            appConfig.put("service_running", SmsReceiverService.isServiceExpectedToRun(context));
            appConfig.put("service_start_count", SmsReceiverService.getServiceStartCount(context));

            // Forwarding rules count
            try {
                int rulesCount = ForwardingConfig.getAll(context).size();
                appConfig.put("forwarding_rules_count", rulesCount);
            } catch (Exception e) {
                appConfig.put("forwarding_rules_count", 0);
            }

            // Webhook configuration status
            appConfig.put("manual_start_webhook_enabled", AppWebhooksActivity.isManualStartWebhookEnabled(context));
            appConfig.put("auto_start_webhook_enabled", AppWebhooksActivity.isAutoStartWebhookEnabled(context));
            appConfig.put("sim_status_webhook_enabled", AppWebhooksActivity.isSimStatusWebhookEnabled(context));

            // Permission status
            JSONObject permissions = new JSONObject();
            permissions.put("sms", ContextCompat.checkSelfPermission(context,
                    Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED);
            permissions.put("phone_state", ContextCompat.checkSelfPermission(context,
                    Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED);
            permissions.put("call_log", ContextCompat.checkSelfPermission(context,
                    Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED);
            permissions.put("contacts", ContextCompat.checkSelfPermission(context,
                    Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED);
            permissions.put("wifi_access", ContextCompat.checkSelfPermission(context,
                    Manifest.permission.ACCESS_WIFI_STATE) == PackageManager.PERMISSION_GRANTED);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissions.put("notifications", ContextCompat.checkSelfPermission(context,
                        Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED);
            }

            appConfig.put("permissions", permissions);

        } catch (Exception e) {
            Log.e(TAG, "Error collecting app config", e);
        }

        return appConfig;
    }

    /**
     * Map SIM state integer to readable string
     */
    private static String mapSimState(int simState) {
        switch (simState) {
            case TelephonyManager.SIM_STATE_ABSENT:
                return "ABSENT";
            case TelephonyManager.SIM_STATE_CARD_IO_ERROR:
                return "CARD_IO_ERROR";
            case TelephonyManager.SIM_STATE_CARD_RESTRICTED:
                return "CARD_RESTRICTED";
            case TelephonyManager.SIM_STATE_NETWORK_LOCKED:
                return "NETWORK_LOCKED";
            case TelephonyManager.SIM_STATE_NOT_READY:
                return "NOT_READY";
            case TelephonyManager.SIM_STATE_PERM_DISABLED:
                return "PERM_DISABLED";
            case TelephonyManager.SIM_STATE_PIN_REQUIRED:
                return "PIN_REQUIRED";
            case TelephonyManager.SIM_STATE_PUK_REQUIRED:
                return "PUK_REQUIRED";
            case TelephonyManager.SIM_STATE_READY:
                return "READY";
            case TelephonyManager.SIM_STATE_UNKNOWN:
                return "UNKNOWN";
            default:
                return "UNKNOWN_" + simState;
        }
    }

    /**
     * Map phone type integer to readable string
     */
    private static String mapPhoneType(int phoneType) {
        switch (phoneType) {
            case TelephonyManager.PHONE_TYPE_CDMA:
                return "CDMA";
            case TelephonyManager.PHONE_TYPE_GSM:
                return "GSM";
            case TelephonyManager.PHONE_TYPE_SIP:
                return "SIP";
            case TelephonyManager.PHONE_TYPE_NONE:
                return "NONE";
            default:
                return "UNKNOWN_" + phoneType;
        }
    }

    /**
     * Map network type integer to readable string
     */
    private static String mapNetworkType(int networkType) {
        switch (networkType) {
            case TelephonyManager.NETWORK_TYPE_1xRTT:
                return "1xRTT";
            case TelephonyManager.NETWORK_TYPE_CDMA:
                return "CDMA";
            case TelephonyManager.NETWORK_TYPE_EDGE:
                return "EDGE";
            case TelephonyManager.NETWORK_TYPE_EHRPD:
                return "EHRPD";
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
                return "EVDO_0";
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
                return "EVDO_A";
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
                return "EVDO_B";
            case TelephonyManager.NETWORK_TYPE_GPRS:
                return "GPRS";
            case TelephonyManager.NETWORK_TYPE_HSDPA:
                return "HSDPA";
            case TelephonyManager.NETWORK_TYPE_HSPA:
                return "HSPA";
            case TelephonyManager.NETWORK_TYPE_HSPAP:
                return "HSPAP";
            case TelephonyManager.NETWORK_TYPE_HSUPA:
                return "HSUPA";
            case TelephonyManager.NETWORK_TYPE_IDEN:
                return "IDEN";
            case TelephonyManager.NETWORK_TYPE_LTE:
                return "LTE";
            case TelephonyManager.NETWORK_TYPE_UMTS:
                return "UMTS";
            case TelephonyManager.NETWORK_TYPE_UNKNOWN:
                return "UNKNOWN";
            default:
                return "UNKNOWN_" + networkType;
        }
    }
}