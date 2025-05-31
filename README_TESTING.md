# Android Nomad Gateway - Testing Setup

Complete testing environment for SMS, Call, and Push notification forwarding with automated scripts and webhook integration.

## 🚀 Quick Start

### 1. Setup Test Environment
```bash
./setup_test_environment.sh
```
This script will:
- Start/verify emulator is running
- Install the app
- Grant all necessary permissions
- Enable notification access
- Test webhook connectivity

### 2. Configure Forwarding Rules
Open the app on the emulator and create three forwarding rules:

#### 📱 SMS Rule
- **Activity Type**: SMS
- **All sources**: Enabled
- **Webhook URL**: `https://static-conv-n8n.teknologia.org/webhook-test/android-activity`
- **Template**:
```json
{
  "from": "%from%",
  "text": "%text%",
  "timestamp": "%sentStamp%",
  "sim": "%sim%",
  "type": "sms"
}
```

#### 📞 Call Rule
- **Activity Type**: Calls
- **All sources**: Enabled
- **Webhook URL**: `https://static-conv-n8n.teknologia.org/webhook-test/android-activity`
- **Template**:
```json
{
  "from": "%from%",
  "contact": "%contact%",
  "timestamp": "%timestamp%",
  "duration": "%duration%",
  "type": "call"
}
```

#### 🔔 Push Rule
- **Activity Type**: Push
- **All sources**: Enabled
- **Webhook URL**: `https://static-conv-n8n.teknologia.org/webhook-test/android-activity`
- **Template**:
```json
{
  "app": "%package%",
  "title": "%title%",
  "content": "%content%",
  "message": "%text%",
  "timestamp": "%sentStamp%",
  "type": "push"
}
```

### 3. Run Tests
```bash
./run_all_tests.sh
```

## 📁 Testing Files

### Configuration
- `test_config.json` - Test configuration with webhook URL and expected templates
- `setup_test_environment.sh` - Complete environment setup script

### Individual Test Scripts
- `test_sms.sh` - SMS forwarding test
- `test_calls.sh` - Call forwarding test  
- `test_push.sh` - Push notification forwarding test

### Comprehensive Testing
- `run_all_tests.sh` - Runs all tests in sequence with verification
- `TESTING_GUIDE.md` - Detailed testing documentation

## 🔧 Webhook Configuration

**Endpoint**: `https://static-conv-n8n.teknologia.org/webhook-test/android-activity`

**Note**: The webhook needs to be activated in n8n test mode before testing. Click the "Test workflow" button in n8n, then run the tests.

## 🧪 Test Scenarios

### SMS Testing
- Sends SMS via ADB: `+1234567890 → "Automated test SMS message"`
- Expected webhook payload includes: from, text, timestamp, sim, type
- Tests connectivity with manual webhook call

### Call Testing  
- Simulates incoming call: `+1987654321`
- Lets call ring for 8 seconds to trigger detection
- Cancels call and checks for webhook payload
- Expected payload includes: from, contact, timestamp, duration, type

### Push Notification Testing
- Uses multiple methods to trigger notifications:
  1. Direct notification command
  2. Chrome app launch
  3. Broadcast intent
  4. Service call
- Checks notification access permissions
- Expected payload includes: app, title, content, message, timestamp, type

## 🔍 Debugging

### Check App Logs
```bash
adb logcat | grep 'IncomingActivityGateway'
```

### Verify Permissions
```bash
adb shell dumpsys package tech.wdg.incomingactivitygateway | grep permission
```

### Check Services
```bash
adb shell dumpsys activity services | grep 'tech.wdg.incomingactivitygateway'
```

### Verify Notification Access
```bash
adb shell settings get secure enabled_notification_listeners
```

### Manual Webhook Test
```bash
curl -X POST 'https://static-conv-n8n.teknologia.org/webhook-test/android-activity' \
  -H 'Content-Type: application/json' \
  -d '{"test":"manual","timestamp":"'$(date +%s)'"}'
```

## 📱 Emulator Commands

### Start Emulator
```bash
export ANDROID_HOME=~/Library/Android/sdk
export PATH=$PATH:$ANDROID_HOME/emulator:$ANDROID_HOME/platform-tools
emulator -avd Medium_Phone_API_36.0 -no-snapshot-load
```

### SMS Simulation
```bash
# Via ADB
adb emu sms send +1234567890 "Test message"

# Via telnet console
telnet localhost 5554
sms send +1234567890 "Test message"
exit
```

### Call Simulation
```bash
# Via ADB
adb emu gsm call +1234567890
adb emu gsm cancel +1234567890

# Via telnet console
telnet localhost 5554
gsm call +1234567890
gsm cancel +1234567890
exit
```

## 🎯 Expected Results

When tests are successful, the webhook should receive:

### SMS Payload
```json
{
  "from": "+1234567890",
  "text": "Automated test SMS message - [timestamp]",
  "timestamp": "[unix_timestamp]",
  "sim": "sim1",
  "type": "sms"
}
```

### Call Payload
```json
{
  "from": "+1987654321",
  "contact": "Unknown",
  "timestamp": "[unix_timestamp]",
  "duration": "[seconds]",
  "type": "call"
}
```

### Push Payload
```json
{
  "app": "com.android.chrome",
  "title": "Test Push Notification",
  "content": "Test notification from script",
  "message": "[notification_text]",
  "timestamp": "[unix_timestamp]",
  "type": "push"
}
```

## ⚠️ Important Notes

1. **Webhook Activation**: The n8n webhook must be in test mode and activated before running tests
2. **Permissions**: All critical permissions are automatically granted by the setup script
3. **Notification Access**: Must be manually enabled in Android settings for push notifications
4. **App Configuration**: Forwarding rules must be created in the app before testing
5. **Emulator**: Tests require a running Android emulator with Google Play Services

## 🔄 Troubleshooting

### No SMS Received
- Check if default SMS app is set
- Verify phone number format
- Check emulator console connection

### No Call Events
- Ensure phone permissions are granted
- Verify call is ringing (not just dialing)
- Check CallBroadcastReceiver registration

### No Push Notifications
- Enable notification access in Settings
- Install notification-capable apps
- Check NotificationListenerService status

### Webhook Issues
- Verify internet connection in emulator
- Check webhook URL is correct and activated
- Test webhook manually with curl
- Check app logs for HTTP errors 