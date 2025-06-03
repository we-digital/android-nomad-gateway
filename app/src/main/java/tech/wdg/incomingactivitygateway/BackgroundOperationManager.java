package tech.wdg.incomingactivitygateway;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;

/**
 * Utility class for managing background operation settings
 * Provides methods to check and guide users for optimal background performance
 */
public class BackgroundOperationManager {

    private static final String TAG = "BackgroundOpManager";

    /**
     * Checks if battery optimization is disabled for the app
     */
    public static boolean isBatteryOptimizationDisabled(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            return pm != null && pm.isIgnoringBatteryOptimizations(context.getPackageName());
        }
        return true; // Not applicable for older versions
    }

    /**
     * Opens battery optimization settings for the app
     */
    @SuppressLint("BatteryLife")
    public static void openBatteryOptimizationSettings(Context context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + context.getPackageName()));
                context.startActivity(intent);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to open battery optimization settings", e);
            // Fallback to general battery optimization settings
            openGeneralBatterySettings(context);
        }
    }

    /**
     * Opens general battery optimization settings
     */
    public static void openGeneralBatterySettings(Context context) {
        try {
            Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
            context.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Failed to open general battery settings", e);
        }
    }

    /**
     * Opens app-specific settings where users can disable battery optimization
     */
    public static void openAppSettings(Context context) {
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
            context.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Failed to open app settings", e);
        }
    }

    /**
     * Gets a user-friendly message about background operation status
     */
    public static String getBackgroundOperationStatus(Context context) {
        StringBuilder status = new StringBuilder();

        // Check battery optimization
        if (!isBatteryOptimizationDisabled(context)) {
            status.append("⚠️ Battery optimization is enabled - may affect background operation\n");
        } else {
            status.append("✅ Battery optimization is disabled\n");
        }

        // Check if service is running
        boolean serviceRunning = SmsReceiverService.isServiceExpectedToRun(context);
        if (serviceRunning) {
            status.append("✅ Background service is configured to run\n");
        } else {
            status.append("❌ Background service is not configured\n");
        }

        return status.toString().trim();
    }

    /**
     * Gets recommendations for better background operation
     */
    public static String getBackgroundOperationRecommendations(Context context) {
        StringBuilder recommendations = new StringBuilder();
        recommendations.append("For optimal background operation:\n\n");

        if (!isBatteryOptimizationDisabled(context)) {
            recommendations.append("1. Disable battery optimization for this app\n");
        }

        recommendations.append("2. Keep the app in recent apps (don't swipe away)\n");
        recommendations.append("3. Enable 'Auto-start' in your device's app management\n");
        recommendations.append("4. Disable 'Adaptive Battery' or add this app to exceptions\n");
        recommendations.append("5. Set battery usage to 'Don't optimize' or 'No restrictions'\n");

        // Device-specific recommendations
        String manufacturer = Build.MANUFACTURER.toLowerCase();
        if (manufacturer.contains("xiaomi") || manufacturer.contains("redmi")) {
            recommendations.append("\nXiaomi/Redmi specific:\n");
            recommendations.append("• Enable 'Autostart' in Security app\n");
            recommendations.append("• Set battery saver to 'No restrictions'\n");
        } else if (manufacturer.contains("huawei") || manufacturer.contains("honor")) {
            recommendations.append("\nHuawei/Honor specific:\n");
            recommendations.append("• Enable 'Auto-launch' in Phone Manager\n");
            recommendations.append("• Add to 'Protected apps' list\n");
        } else if (manufacturer.contains("oppo") || manufacturer.contains("oneplus")) {
            recommendations.append("\nOppo/OnePlus specific:\n");
            recommendations.append("• Enable 'Auto-start' in Settings\n");
            recommendations.append("• Disable battery optimization\n");
        } else if (manufacturer.contains("vivo")) {
            recommendations.append("\nVivo specific:\n");
            recommendations.append("• Enable 'High background app limit'\n");
            recommendations.append("• Add to 'Whitelist' in iManager\n");
        }

        return recommendations.toString();
    }

    /**
     * Logs current background operation status for debugging
     */
    public static void logBackgroundOperationStatus(Context context) {
        Log.d(TAG, "=== Background Operation Status ===");
        Log.d(TAG, "Battery optimization disabled: " + isBatteryOptimizationDisabled(context));
        Log.d(TAG, "Service expected to run: " + SmsReceiverService.isServiceExpectedToRun(context));
        Log.d(TAG, "Service start count: " + SmsReceiverService.getServiceStartCount(context));
        Log.d(TAG, "Device manufacturer: " + Build.MANUFACTURER);
        Log.d(TAG, "Android version: " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")");
        Log.d(TAG, "=================================");
    }
}