#!/bin/bash
# Setup Test Environment for Android Nomad Gateway

export ANDROID_HOME=~/Library/Android/sdk
export PATH=$PATH:$ANDROID_HOME/emulator:$ANDROID_HOME/platform-tools

WEBHOOK_URL="https://example.com/test_webhook"
PACKAGE_NAME="tech.wdg.incomingactivitygateway"

echo "🚀 Setting up Android Nomad Gateway Test Environment"
echo "=================================================="
echo ""

# Check if emulator is running
echo "🔍 Checking emulator status..."
adb devices | grep -q "emulator"
if [ $? -ne 0 ]; then
    echo "❌ No emulator detected. Starting emulator..."
    emulator -avd Medium_Phone_API_36.0 -no-snapshot-load &
    echo "⏱️  Waiting for emulator to boot..."
    adb wait-for-device
    sleep 10
fi

echo "✅ Emulator is ready!"
echo ""

# Install the app
echo "📱 Installing app..."
if [ -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
    adb install -r app/build/outputs/apk/debug/app-debug.apk
    echo "✅ App installed successfully!"
else
    echo "❌ APK not found. Building app..."
    ./gradlew assembleDebug
    adb install -r app/build/outputs/apk/debug/app-debug.apk
fi

echo ""
echo "🔧 Configuring permissions..."

# Grant SMS permissions
echo "📱 Granting SMS permissions..."
adb shell pm grant $PACKAGE_NAME android.permission.RECEIVE_SMS
adb shell pm grant $PACKAGE_NAME android.permission.READ_SMS
adb shell pm grant $PACKAGE_NAME android.permission.SEND_SMS

# Grant phone permissions
echo "📞 Granting phone permissions..."
adb shell pm grant $PACKAGE_NAME android.permission.READ_PHONE_STATE
adb shell pm grant $PACKAGE_NAME android.permission.READ_CALL_LOG
adb shell pm grant $PACKAGE_NAME android.permission.READ_CONTACTS

# Grant notification access
echo "🔔 Enabling notification access..."
adb shell settings put secure enabled_notification_listeners "$PACKAGE_NAME/.NotificationListener"

# Grant other necessary permissions
echo "🔐 Granting additional permissions..."
adb shell pm grant $PACKAGE_NAME android.permission.INTERNET
adb shell pm grant $PACKAGE_NAME android.permission.ACCESS_NETWORK_STATE
adb shell pm grant $PACKAGE_NAME android.permission.WAKE_LOCK

echo "✅ All permissions granted!"
echo ""

# Test webhook connectivity
echo "🌐 Testing webhook connectivity..."
curl -s -X POST "$WEBHOOK_URL" \
    -H "Content-Type: application/json" \
    -H "User-Agent: Android-Nomad-Gateway-Setup/1.0" \
    -d '{
        "test": "setup",
        "message": "Testing webhook connectivity from setup script",
        "timestamp": "'$(date +%s)'",
        "device": "emulator"
    }' && echo "✅ Webhook is accessible" || echo "⚠️  Webhook connectivity issue"

echo ""
echo "📋 Configuration Summary:"
echo "   • Webhook URL: $WEBHOOK_URL"
echo "   • Package: $PACKAGE_NAME"
echo "   • Emulator: Ready"
echo "   • Permissions: Granted"
echo ""

echo "🎯 Next Steps:"
echo "1. Open the app on the emulator"
echo "2. Create forwarding rules with the following settings:"
echo ""
echo "   📱 SMS Rule:"
echo "   - Activity Type: SMS"
echo "   - All sources: Enabled"
echo "   - Webhook URL: $WEBHOOK_URL"
echo "   - Template: {\"from\":\"%from%\",\"text\":\"%text%\",\"timestamp\":\"%sentStamp%\",\"sim\":\"%sim%\",\"type\":\"sms\"}"
echo ""
echo "   📞 Call Rule:"
echo "   - Activity Type: Calls"
echo "   - All sources: Enabled"
echo "   - Webhook URL: $WEBHOOK_URL"
echo "   - Template: {\"from\":\"%from%\",\"contact\":\"%contact%\",\"timestamp\":\"%timestamp%\",\"duration\":\"%duration%\",\"type\":\"call\"}"
echo ""
echo "   🔔 Push Rule:"
echo "   - Activity Type: Push"
echo "   - All sources: Enabled"
echo "   - Webhook URL: $WEBHOOK_URL"
echo "   - Template: {\"app\":\"%package%\",\"title\":\"%title%\",\"content\":\"%content%\",\"message\":\"%text%\",\"timestamp\":\"%sentStamp%\",\"type\":\"push\"}"
echo ""
echo "3. Run tests with: ./run_all_tests.sh"
echo ""
echo "🔍 Useful commands:"
echo "   • Check logs: adb logcat | grep 'IncomingActivityGateway'"
echo "   • Check services: adb shell dumpsys activity services | grep '$PACKAGE_NAME'"
echo "   • Check permissions: adb shell dumpsys package $PACKAGE_NAME | grep permission" 