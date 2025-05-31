package tech.wdg.incomingactivitygateway;

import android.app.Application;
import android.content.ComponentCallbacks2;
import android.content.res.Configuration;
import android.os.Build;
import android.util.Log;

import java.io.File;

public class GatewayApplication extends Application {

    private static final String TAG = "GatewayApplication";

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize memory management
        initializeMemoryManagement();

        // Clean up cache directories
        cleanupCacheDirectories();

        Log.d(TAG, "Gateway Application initialized");
    }

    private void initializeMemoryManagement() {
        // Register for memory trim callbacks
        registerComponentCallbacks(new ComponentCallbacks2() {
            @Override
            public void onTrimMemory(int level) {
                Log.d(TAG, "Memory trim requested: level " + level);

                switch (level) {
                    case ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN:
                        // App UI is hidden, release UI-related resources
                        break;
                    case ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE:
                    case ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW:
                    case ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL:
                        // App is running but system is low on memory
                        performMemoryCleanup();
                        break;
                    case ComponentCallbacks2.TRIM_MEMORY_BACKGROUND:
                    case ComponentCallbacks2.TRIM_MEMORY_MODERATE:
                    case ComponentCallbacks2.TRIM_MEMORY_COMPLETE:
                        // App is in background, aggressive cleanup
                        performAggressiveCleanup();
                        break;
                }
            }

            @Override
            public void onConfigurationChanged(Configuration newConfig) {
                // Handle configuration changes
            }

            @Override
            public void onLowMemory() {
                Log.w(TAG, "Low memory warning received");
                performAggressiveCleanup();
            }
        });
    }

    private void performMemoryCleanup() {
        try {
            // Modern memory management - let the system handle GC
            // System.gc() is discouraged in modern Android

            // Clear any cached data
            clearInternalCaches();

            Log.d(TAG, "Memory cleanup performed");
        } catch (Exception e) {
            Log.e(TAG, "Error during memory cleanup", e);
        }
    }

    private void performAggressiveCleanup() {
        try {
            performMemoryCleanup();

            // Additional cleanup for background state
            clearTemporaryFiles();

            Log.d(TAG, "Aggressive cleanup performed");
        } catch (Exception e) {
            Log.e(TAG, "Error during aggressive cleanup", e);
        }
    }

    private void clearInternalCaches() {
        try {
            // Clear internal cache directory more safely
            File cacheDir = getCacheDir();
            if (cacheDir != null && cacheDir.exists()) {
                clearCacheDirectory(cacheDir);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error clearing internal caches", e);
        }
    }

    private void clearCacheDirectory(File dir) {
        if (dir == null || !dir.exists() || !dir.isDirectory()) {
            return;
        }

        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    clearCacheDirectory(file);
                }
                try {
                    if (!file.delete()) {
                        Log.w(TAG, "Failed to delete cache file: " + file.getName());
                    }
                } catch (SecurityException e) {
                    Log.w(TAG, "Security exception deleting cache file: " + file.getName(), e);
                }
            }
        }
    }

    private void clearTemporaryFiles() {
        try {
            // Clear temporary files
            File tempDir = new File(getCacheDir(), "temp");
            if (tempDir.exists()) {
                clearCacheDirectory(tempDir);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error clearing temporary files", e);
        }
    }

    private void cleanupCacheDirectories() {
        try {
            // Clean up code cache directory that's causing issues
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                File codeCacheDir = getCodeCacheDir();
                if (codeCacheDir != null) {
                    File llDir = new File(codeCacheDir, ".ll");
                    if (llDir.exists()) {
                        clearCacheDirectory(llDir);
                        Log.d(TAG, "Cleaned up .ll directory");
                    }
                }
            }

            // Ensure cache directories exist
            ensureCacheDirectoriesExist();

        } catch (Exception e) {
            Log.e(TAG, "Error during cache cleanup", e);
        }
    }

    private void ensureCacheDirectoriesExist() {
        try {
            // Ensure main cache directory exists
            File cacheDir = getCacheDir();
            if (cacheDir != null && !cacheDir.exists()) {
                if (!cacheDir.mkdirs()) {
                    Log.w(TAG, "Failed to create cache directory");
                }
            }

            // Ensure code cache directory exists (API 21+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                File codeCacheDir = getCodeCacheDir();
                if (codeCacheDir != null && !codeCacheDir.exists()) {
                    if (!codeCacheDir.mkdirs()) {
                        Log.w(TAG, "Failed to create code cache directory");
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error ensuring cache directories exist", e);
        }
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        Log.d(TAG, "Gateway Application terminated");
    }
}