# Android Nomad Gateway - Permissions Guide

## Overview
This document provides a comprehensive overview of all permissions required by the Android Nomad Gateway app and their purposes. The app now displays all permission statuses in the Settings activity for complete transparency.

## Required Permissions

### 🔐 Critical Permissions (Required for Core Functionality)

#### 1. SMS Permissions
- **Permission**: `android.permission.RECEIVE_SMS`
- **Purpose**: Receive and forward SMS messages to configured webhooks
- **Status**: Displayed in Settings → Permissions Status → "SMS Access"
- **Required**: Yes - Core functionality

#### 2. Phone State Access
- **Permission**: `android.permission.READ_PHONE_STATE`
- **Purpose**: Access phone state information and SIM card details
- **Status**: Displayed in Settings → Permissions Status → "Phone State"
- **Required**: Yes - For SIM identification and call monitoring

#### 3. Call Log Access
- **Permission**: `android.permission.READ_CALL_LOG`
- **Purpose**: Monitor incoming calls for call forwarding rules
- **Status**: Displayed in Settings → Permissions Status → "Call Log"
- **Required**: Yes - For call event forwarding

#### 4. Contacts Access
- **Permission**: `android.permission.READ_CONTACTS`
- **Purpose**: Resolve phone numbers to contact names in forwarded messages
- **Status**: Displayed in Settings → Permissions Status → "Contacts"
- **Required**: Optional - Enhances message context

#### 5. Phone Numbers Access
- **Permission**: `android.permission.READ_PHONE_NUMBERS`
- **Purpose**: Read device phone numbers for SIM identification
- **Status**: Displayed in Settings → Permissions Status → "Phone Numbers"
- **Required**: Yes - For accurate SIM information

### 📱 Notification Permissions

#### 6. Post Notifications (Android 13+)
- **Permission**: `android.permission.POST_NOTIFICATIONS`
- **Purpose**: Display service status and forwarding notifications
- **Status**: Displayed in Settings → Permissions Status → "Notifications"
- **Required**: Yes on Android 13+ - For user notifications
- **Note**: Only visible on Android 13+ devices

#### 7. Notification Listener Service
- **Permission**: `android.permission.BIND_NOTIFICATION_LISTENER_SERVICE`
- **Purpose**: Access and forward push notifications from other apps
- **Status**: Displayed in Settings → Permissions Status → "Notification Access"
- **Required**: Yes - For push notification forwarding
- **Special**: Requires manual activation in system settings

### 🌐 Network Permissions (Automatically Granted)

#### 8. Internet Access
- **Permission**: `android.permission.INTERNET`
- **Purpose**: Send HTTP requests to configured webhook endpoints
- **Status**: Automatically granted (not displayed)
- **Required**: Yes - Core functionality

#### 9. Network State Access
- **Permission**: `android.permission.ACCESS_NETWORK_STATE`
- **Purpose**: Check network connectivity before sending requests
- **Status**: Automatically granted (not displayed)
- **Required**: Yes - For reliable message delivery

### ⚙️ Service Permissions (System Level)

#### 10. Foreground Service
- **Permission**: `android.permission.FOREGROUND_SERVICE`
- **Purpose**: Run background service for continuous monitoring
- **Status**: System permission (not displayed)
- **Required**: Yes - For background operation

#### 11. Foreground Service Data Sync
- **Permission**: `android.permission.FOREGROUND_SERVICE_DATA_SYNC`
- **Purpose**: Specify foreground service type for data synchronization
- **Status**: System permission (not displayed)
- **Required**: Yes - Modern Android requirement

#### 12. Wake Lock
- **Permission**: `android.permission.WAKE_LOCK`
- **Purpose**: Keep device awake during message processing
- **Status**: System permission (not displayed)
- **Required**: Yes - For reliable message handling

#### 13. Boot Completed
- **Permission**: `android.permission.RECEIVE_BOOT_COMPLETED`
- **Purpose**: Automatically start service after device reboot
- **Status**: System permission (not displayed)
- **Required**: Yes - For persistent operation

## Permission Status Display

### Settings Activity Enhancement
The Settings activity now displays comprehensive permission status:

1. **Real-time Status**: All permissions are checked in real-time
2. **Visual Indicators**: 
   - 🟢 **Green "Granted"** - Permission is active
   - 🔴 **Red "Denied"** - Permission needs to be granted
3. **Interactive Chips**: Tap any permission chip to open relevant settings
4. **Automatic Updates**: Status refreshes when returning to the activity

### Permission Categories in Settings

#### Core Permissions Section
- SMS Access
- Phone State
- Call Log
- Contacts
- Phone Numbers

#### Notification Permissions Section
- Notifications (Android 13+ only)
- Notification Access (Special permission)

### User Actions

#### For Regular Permissions
- **Tap any permission chip** → Opens App Settings
- **Navigate to**: Settings → Apps → Activity Gateway → Permissions
- **Grant required permissions** for full functionality

#### For Notification Listener
- **Tap "Notification Access" chip** → Opens Notification Listener Settings
- **Navigate to**: Settings → Apps & notifications → Special app access → Notification access
- **Enable "Activity Gateway"** for push notification forwarding

## Permission Requirements by Feature

### SMS Forwarding
**Required Permissions**:
- ✅ SMS Access
- ✅ Phone State
- ✅ Phone Numbers
- ⚪ Contacts (optional - for contact name resolution)

### Call Forwarding
**Required Permissions**:
- ✅ Call Log
- ✅ Phone State
- ✅ Phone Numbers
- ⚪ Contacts (optional - for contact name resolution)

### Push Notification Forwarding
**Required Permissions**:
- ✅ Notification Access (special permission)
- ✅ Notifications (Android 13+)

### Background Operation
**Required Permissions**:
- ✅ All service permissions (automatically handled)
- ✅ Internet access
- ✅ Network state access

## Troubleshooting

### Common Issues

#### "SMS not being forwarded"
1. Check SMS Access permission status
2. Verify service is running (Settings → Service Status)
3. Ensure forwarding rules are configured

#### "Calls not being detected"
1. Check Call Log permission status
2. Check Phone State permission status
3. Verify call forwarding rules are enabled

#### "Push notifications not forwarding"
1. Check Notification Access permission (special)
2. Verify notification forwarding rules
3. Ensure target apps are not excluded

#### "Service stops after reboot"
1. Boot Completed permission should be automatically granted
2. Check if battery optimization is disabled for the app
3. Verify service auto-start is enabled

### Permission Denied Solutions

#### If permissions are denied:
1. **Open Settings** → Tap any red "Denied" chip
2. **Grant permissions** in the system settings
3. **Return to app** → Status will update automatically
4. **Restart service** if needed

#### For Notification Listener:
1. **Tap "Notification Access"** chip in Settings
2. **Find "Activity Gateway"** in the list
3. **Toggle ON** the permission
4. **Return to app** → Status will update

## Security & Privacy

### Data Handling
- **Local Processing**: All permission checks are done locally
- **No Data Collection**: Permission status is not transmitted
- **User Control**: All permissions can be revoked at any time

### Privacy Features
- **Minimal Permissions**: Only requests necessary permissions
- **Transparent Display**: All permissions clearly shown in Settings
- **User Choice**: Optional permissions clearly marked
- **Secure Storage**: Sensitive data excluded from backups

## Best Practices

### For Users
1. **Grant Core Permissions**: SMS, Phone State, Call Log for basic functionality
2. **Enable Notification Access**: For push notification forwarding
3. **Review Regularly**: Check permission status in Settings
4. **Understand Impact**: Know what each permission enables

### For Developers
1. **Request Minimally**: Only request necessary permissions
2. **Explain Clearly**: Document purpose of each permission
3. **Handle Gracefully**: App functions with partial permissions
4. **Update Transparently**: Show real-time permission status

## Conclusion

The Android Nomad Gateway app now provides complete transparency about all required permissions through the enhanced Settings activity. Users can easily see which permissions are granted, which are needed, and can quickly access system settings to make changes.

This comprehensive permission system ensures:
- ✅ **Full Functionality** with all permissions granted
- ✅ **Graceful Degradation** with partial permissions
- ✅ **User Transparency** with clear status display
- ✅ **Easy Management** with direct settings access

The app respects user privacy while providing powerful message forwarding capabilities across SMS, calls, and push notifications. 