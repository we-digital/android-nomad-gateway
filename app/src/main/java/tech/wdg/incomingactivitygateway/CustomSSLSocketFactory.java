package tech.wdg.incomingactivitygateway;

import android.content.Context;
import android.util.Log;

import javax.net.ssl.SSLSocketFactory;

import tech.wdg.incomingactivitygateway.SSLSocketFactory.TLSSocketFactory;

/**
 * Singleton wrapper for custom SSL socket factory
 */
public class CustomSSLSocketFactory {
    private static final String TAG = "CustomSSLSocketFactory";
    private static SSLSocketFactory instance;

    /**
     * Get the custom SSL socket factory instance
     */
    public static synchronized SSLSocketFactory getInstance(Context context) {
        if (instance == null) {
            try {
                // Create TLSSocketFactory with secure mode (false = don't ignore SSL)
                instance = new TLSSocketFactory(false);
                Log.d(TAG, "Custom SSL socket factory initialized");
            } catch (Exception e) {
                Log.e(TAG, "Failed to create custom SSL socket factory", e);
                // Fall back to default
                instance = (SSLSocketFactory) SSLSocketFactory.getDefault();
            }
        }
        return instance;
    }

    /**
     * Reset the instance (useful for testing)
     */
    public static synchronized void reset() {
        instance = null;
    }
}