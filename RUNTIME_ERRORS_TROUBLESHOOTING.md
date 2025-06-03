# Runtime Errors Troubleshooting Guide

## Overview

This guide addresses the runtime errors you're experiencing with your SMS Gateway app on Android 16 (API 36). These are **not build errors** but runtime permission and system integration issues.

## 🚨 **Critical Issues Identified**

### 1. **Foreground Service Permission Denied**
```
E  Operation not started: uid=10217 pkg=tech.wdg.incomingactivitygateway(null) op=START_FOREGROUND
```

**Root Cause**: Android 14+ (API 34+) has stricter foreground service requirements.

**Solutions Implemented**:
- ✅ Added `FOREGROUND_SERVICE_DATA_SYNC` permission
- ✅ Added `FOREGROUND_SERVICE_SPECIAL_USE` permission  
- ✅ Updated service manifest with `specialUse` type
- ✅ Added meta-data for special use justification
- ✅ Enhanced error handling in service startup

### 2. **APK Asset Loading Errors**
```
E  Failed to open APK '/data/app/.../base.apk': I/O error
```

**Root Cause**: Rapid app restarts or installation corruption.

**Solutions**:
- Clean and rebuild the project
- Uninstall and reinstall the app
- Clear app data and cache

### 3. **Dead Object Exceptions**
```
E  unable to notify listener: android.os.DeadObjectException
```

**Root Cause**: NotificationListenerService being killed by system.

**Solutions Implemented**:
- ✅ Enhanced service restart mechanism
- ✅ Better error handling in service lifecycle
- ✅ Improved state tracking

## 🛠️ **Immediate Actions Required**

### Step 1: Clean Build
```bash
./gradlew clean
./gradlew build
```

### Step 2: Uninstall and Reinstall
```bash
adb uninstall tech.wdg.incomingactivitygateway
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Step 3: Grant Permissions Manually
1. Go to **Settings → Apps → SMS Gateway**
2. **Permissions → Allow all requested permissions**
3. **Special app access → Battery optimization → Don't optimize**
4. **Special app access → Notification access → Enable**

### Step 4: Check Foreground Service Permissions
For Android 14+:
1. **Settings → Apps → SMS Gateway → Permissions**
2. Look for **"Foreground services"** permission
3. Ensure it's enabled

## 🔧 **Technical Fixes Implemented**

### Enhanced Manifest Configuration
```xml
<!-- Added Android 14+ foreground service permissions -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />

<!-- Enhanced service configuration -->
<service
    android:name=".SmsReceiverService"
    android:foregroundServiceType="dataSync|specialUse">
    <meta-data
        android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
        android:value="SMS and call monitoring for message forwarding" />
</service>
```

### Enhanced Service Error Handling
```java
private void startForegroundWithNotification() {
    try {
        Notification notification = createServiceNotification();
        startForeground(NOTIFICATION_ID, notification);
    } catch (SecurityException e) {
        Log.e(TAG, "Foreground service permission denied", e);
        handleForegroundServicePermissionDenied();
    }
}
```

### Permission Request Updates
- Added Android 14+ foreground service permissions to request flow
- Enhanced permission explanation dialog
- Better error handling for permission denials

## 📱 **Device-Specific Issues**

### Android 16 (API 36) Considerations
Your device is running a very new Android version with stricter requirements:

1. **Foreground Service Restrictions**: More stringent permission checks
2. **Background App Limits**: Aggressive app killing policies
3. **Security Enhancements**: Stricter APK validation

### Google Pixel Specific
Since you're on a Google device:
1. **Adaptive Battery**: May be aggressively killing your app
2. **App Standby**: May put your app in standby mode
3. **Background App Refresh**: May be disabled

## 🔍 **Debugging Steps**

### Check Current Status
```bash
# Check if service is running
adb shell dumpsys activity services tech.wdg.incomingactivitygateway

# Check app permissions
adb shell dumpsys package tech.wdg.incomingactivitygateway | grep permission

# Check battery optimization
adb shell dumpsys deviceidle whitelist | grep tech.wdg.incomingactivitygateway
```

### Monitor Logs
```bash
# Filter for your app logs
adb logcat | grep "tech.wdg.incomingactivitygateway"

# Check for permission errors
adb logcat | grep "Operation not started"

# Monitor service lifecycle
adb logcat | grep "SmsReceiverService"
```

## ⚡ **Quick Fixes**

### 1. Force Stop and Restart
```bash
adb shell am force-stop tech.wdg.incomingactivitygateway
adb shell am start -n tech.wdg.incomingactivitygateway/.MainActivity
```

### 2. Clear App Data
```bash
adb shell pm clear tech.wdg.incomingactivitygateway
```

### 3. Disable Battery Optimization
```bash
adb shell dumpsys deviceidle whitelist +tech.wdg.incomingactivitygateway
```

## 🎯 **Expected Behavior After Fixes**

After implementing these fixes, you should see:

1. **No more "Operation not started" errors**
2. **Successful foreground service startup**
3. **Persistent notification showing service status**
4. **Service surviving app swipes and reboots**
5. **Proper error handling and recovery**

## 📋 **Testing Checklist**

- [ ] App installs without errors
- [ ] All permissions granted successfully
- [ ] Foreground service starts without permission errors
- [ ] Notification appears and persists
- [ ] Service survives app being swiped away
- [ ] Service restarts after device reboot
- [ ] SMS and call monitoring works correctly
- [ ] No more APK loading errors in logs

## 🆘 **If Issues Persist**

1. **Check Android Version Compatibility**: Ensure your target SDK supports Android 16
2. **Review Device Settings**: Some OEMs have additional restrictions
3. **Test on Different Device**: Try on Android 13-14 device for comparison
4. **Enable Developer Options**: Turn on "Don't keep activities" to test service persistence

## 📞 **Support Information**

If you continue experiencing issues:
1. Provide the output of `adb logcat` after implementing fixes
2. Share device information: `adb shell getprop ro.build.version.release`
3. Check if the issue occurs on other Android versions

The fixes implemented should resolve the foreground service permission issues and improve overall app stability on Android 16. 