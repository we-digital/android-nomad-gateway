# Android Nomad Gateway - Testing Guide

This guide explains how to test SMS, call, and push notification forwarding using the Android emulator.

## Prerequisites

1. **Android Emulator Running**: Start an AVD with Google Play Services
2. **App Installed**: Install the debug APK on the emulator
3. **Webhook Server**: Set up a test webhook endpoint (we'll use webhook.site)

## Setup Instructions

### 1. Start the Emulator
```bash
export ANDROID_HOME=~/Library/Android/sdk
export PATH=$PATH:$ANDROID_HOME/emulator:$ANDROID_HOME/platform-tools
emulator -avd Medium_Phone_API_36.0 -no-snapshot-load
```

### 2. Install the App
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 3. Set Up Test Webhook
1. Go to https://webhook.site
2. Copy the unique URL (e.g., `https://webhook.site/12345678-1234-1234-1234-123456789abc`)
3. Use this URL in your forwarding rules

## Testing Each Feature

### 🔔 Testing Push Notifications

#### Method 1: Using ADB Commands
```bash
# Send a test notification
adb shell am broadcast -a com.android.test.notification \
  --es title "Test Title" \
  --es text "Test notification message" \
  --es package "com.android.chrome"
```

#### Method 2: Using Apps
1. Install apps like Chrome, Gmail, or WhatsApp on the emulator
2. Sign in and enable notifications
3. Send yourself emails or messages to trigger notifications

#### Method 3: Using Notification Tester App
```bash
# Install a notification testing app
adb install path/to/notification-tester.apk
```

### 📱 Testing SMS Messages

#### Method 1: Using Emulator Console
1. Open terminal and connect to emulator console:
```bash
telnet localhost 5554
```

2. Send SMS to emulator:
```
sms send +1234567890 "Test SMS message"
```

3. Exit console:
```
exit
```

#### Method 2: Using ADB Commands
```bash
# Send SMS via ADB
adb emu sms send +1234567890 "Test SMS from ADB"
```

#### Method 3: Using Another Emulator Instance
1. Start a second emulator
2. Use the phone app to send SMS between emulators

### 📞 Testing Phone Calls

#### Method 1: Using Emulator Console
1. Connect to emulator console:
```bash
telnet localhost 5554
```

2. Simulate incoming call:
```
gsm call +1234567890
```

3. End the call:
```
gsm cancel +1234567890
```

#### Method 2: Using ADB Commands
```bash
# Simulate incoming call
adb emu gsm call +1234567890

# End the call
adb emu gsm cancel +1234567890
```

## Testing Workflow

### 1. Create Forwarding Rules

#### SMS Rule
- **Activity Type**: SMS
- **Source**: Specific phone number or "All sources"
- **Webhook URL**: Your webhook.site URL
- **Template**: 
```json
{
  "from": "%from%",
  "text": "%text%",
  "timestamp": "%sentStamp%",
  "sim": "%sim%"
}
```

#### Push Notification Rule
- **Activity Type**: Push
- **Source**: Specific app or "All sources"
- **Webhook URL**: Your webhook.site URL
- **Template**:
```json
{
  "app": "%package%",
  "title": "%title%",
  "content": "%content%",
  "message": "%text%",
  "timestamp": "%sentStamp%"
}
```

#### Call Rule
- **Activity Type**: Calls
- **Source**: Specific phone numbers or "All sources"
- **Webhook URL**: Your webhook.site URL
- **Template**:
```json
{
  "from": "%from%",
  "contact": "%contact%",
  "timestamp": "%timestamp%",
  "duration": "%duration%"
}
```

### 2. Grant Permissions

Make sure to grant all required permissions in the emulator:
- **SMS**: SMS permissions
- **Calls**: Phone and Contacts permissions
- **Push**: Notification access (Settings > Apps > Special access > Notification access)

### 3. Test Each Feature

1. **Test SMS**: Send SMS using telnet/ADB → Check webhook.site for payload
2. **Test Calls**: Simulate call using telnet/ADB → Check webhook.site for payload
3. **Test Push**: Trigger notification → Check webhook.site for payload

## Advanced Testing Scripts

### Automated SMS Testing
```bash
#!/bin/bash
# test_sms.sh
export ANDROID_HOME=~/Library/Android/sdk
export PATH=$PATH:$ANDROID_HOME/platform-tools

echo "Testing SMS forwarding..."
adb emu sms send +1234567890 "Automated test message $(date)"
echo "SMS sent. Check your webhook endpoint."
```

### Automated Call Testing
```bash
#!/bin/bash
# test_calls.sh
export ANDROID_HOME=~/Library/Android/sdk
export PATH=$PATH:$ANDROID_HOME/platform-tools

echo "Testing call forwarding..."
adb emu gsm call +1234567890
sleep 5
adb emu gsm cancel +1234567890
echo "Call simulation complete. Check your webhook endpoint."
```

### Batch Testing
```bash
#!/bin/bash
# batch_test.sh
echo "Running comprehensive tests..."

# Test SMS
./test_sms.sh
sleep 2

# Test Calls
./test_calls.sh
sleep 2

echo "All tests completed. Check your webhook endpoint for results."
```

## Troubleshooting

### Common Issues

1. **No SMS Received**: 
   - Check if SMS app is set as default
   - Verify phone number format
   - Check emulator console connection

2. **No Call Events**:
   - Ensure phone permissions are granted
   - Check if call is actually ringing (not just dialing)
   - Verify phone state broadcast receiver is registered

3. **No Push Notifications**:
   - Enable notification access for the app
   - Install apps that actually send notifications
   - Check notification listener service is running

4. **Webhook Not Receiving Data**:
   - Verify internet connection in emulator
   - Check webhook URL is correct
   - Test webhook URL manually with curl
   - Check app logs for errors

### Debugging Commands

```bash
# Check app logs
adb logcat | grep "IncomingActivityGateway"

# Check if services are running
adb shell dumpsys activity services | grep "tech.wdg.incomingactivitygateway"

# Check notification access
adb shell settings get secure enabled_notification_listeners

# Check SMS permissions
adb shell dumpsys package tech.wdg.incomingactivitygateway | grep permission
```

## Expected Results

When testing is successful, you should see:

1. **SMS**: JSON payload with sender, message content, timestamp, and SIM info
2. **Calls**: JSON payload with caller number, contact name, timestamp, and duration
3. **Push**: JSON payload with app package, title, content, and timestamp

Each payload should appear on your webhook.site URL within seconds of triggering the test event. 