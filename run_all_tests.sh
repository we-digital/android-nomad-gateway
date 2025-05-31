#!/bin/bash
# Comprehensive Testing Script for Android Nomad Gateway

export ANDROID_HOME=~/Library/Android/sdk
export PATH=$PATH:$ANDROID_HOME/emulator:$ANDROID_HOME/platform-tools

WEBHOOK_URL="https://example.com/test_webhook"
PACKAGE_NAME="tech.wdg.incomingactivitygateway"

echo "🚀 Android Nomad Gateway - Comprehensive Testing Suite"
echo "=================================================="
echo "🌐 Webhook endpoint: $WEBHOOK_URL"
echo ""

# Check if emulator is running
echo "🔍 Checking emulator status..."
adb devices | grep -q "emulator"
if [ $? -ne 0 ]; then
    echo "❌ No emulator detected. Please start an emulator first:"
    echo "   ./setup_test_environment.sh"
    exit 1
fi

echo "✅ Emulator detected!"
echo ""

# Check if app is installed
echo "📱 Checking app installation..."
adb shell pm list packages | grep -q "$PACKAGE_NAME"
if [ $? -ne 0 ]; then
    echo "❌ App not installed. Installing now..."
    if [ -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
        adb install -r app/build/outputs/apk/debug/app-debug.apk
        echo "✅ App installed successfully!"
    else
        echo "❌ APK not found. Please build the app first:"
        echo "   ./gradlew assembleDebug"
        exit 1
    fi
else
    echo "✅ App is installed!"
fi

echo ""
echo "🔧 Verifying permissions..."

# Check critical permissions
PERMISSIONS_OK=true

# Check SMS permissions
adb shell dumpsys package $PACKAGE_NAME | grep -q "android.permission.RECEIVE_SMS.*granted=true"
if [ $? -eq 0 ]; then
    echo "✅ SMS permissions granted"
else
    echo "⚠️  SMS permissions missing"
    PERMISSIONS_OK=false
fi

# Check phone permissions
adb shell dumpsys package $PACKAGE_NAME | grep -q "android.permission.READ_PHONE_STATE.*granted=true"
if [ $? -eq 0 ]; then
    echo "✅ Phone permissions granted"
else
    echo "⚠️  Phone permissions missing"
    PERMISSIONS_OK=false
fi

# Check notification access
NOTIFICATION_ACCESS=$(adb shell settings get secure enabled_notification_listeners)
if [[ "$NOTIFICATION_ACCESS" == *"$PACKAGE_NAME"* ]]; then
    echo "✅ Notification access enabled"
else
    echo "⚠️  Notification access not enabled"
    PERMISSIONS_OK=false
fi

if [ "$PERMISSIONS_OK" = false ]; then
    echo ""
    echo "❌ Some permissions are missing. Run setup script first:"
    echo "   ./setup_test_environment.sh"
    echo ""
    read -p "Continue anyway? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

echo ""
echo "⚠️  IMPORTANT: Before running tests, make sure to:"
echo "   1. Open the app and create forwarding rules with webhook: $WEBHOOK_URL"
echo "   2. Use the templates provided in setup_test_environment.sh"
echo "   3. Save the rules and ensure the app is running"
echo ""

read -p "Press Enter when ready to start testing..."

echo ""
echo "🧪 Starting test sequence..."
echo ""

# Test webhook connectivity first
echo "🌐 Testing webhook connectivity..."
curl -s -X POST "$WEBHOOK_URL" \
    -H "Content-Type: application/json" \
    -H "User-Agent: Android-Nomad-Gateway-Test/1.0" \
    -d '{
        "test": "connectivity",
        "message": "Pre-test connectivity check",
        "timestamp": "'$(date +%s)'",
        "source": "test_runner"
    }' && echo "✅ Webhook is accessible" || echo "⚠️  Webhook connectivity issue"

echo ""

# Test 1: SMS
echo "1️⃣  Testing SMS forwarding..."
echo "================================"
./test_sms.sh
echo ""
echo "⏱️  Waiting 5 seconds before next test..."
sleep 5

# Test 2: Calls
echo "2️⃣  Testing call forwarding..."
echo "================================"
./test_calls.sh
echo ""
echo "⏱️  Waiting 5 seconds before next test..."
sleep 5

# Test 3: Push Notifications
echo "3️⃣  Testing push notification forwarding..."
echo "=========================================="
./test_push.sh
echo ""

echo "🎉 All tests completed!"
echo "======================="
echo ""
echo "📊 Results Summary:"
echo "   • SMS test: Check webhook for message data"
echo "   • Call test: Check webhook for call data"
echo "   • Push test: Check webhook for notification data"
echo ""
echo "🌐 Webhook endpoint: $WEBHOOK_URL"
echo ""
echo "🔍 Debugging commands:"
echo "   • View logs: adb logcat | grep 'IncomingActivityGateway'"
echo "   • Check services: adb shell dumpsys activity services | grep '$PACKAGE_NAME'"
echo "   • Check permissions: adb shell dumpsys package $PACKAGE_NAME | grep permission"
echo "   • Check notification access: adb shell settings get secure enabled_notification_listeners"
echo ""
echo "📱 App debugging:"
echo "   • Open app: adb shell am start -n $PACKAGE_NAME/.MainActivity"
echo "   • Check app status: adb shell am force-stop $PACKAGE_NAME && adb shell am start -n $PACKAGE_NAME/.MainActivity"
echo ""
echo "🌐 Manual webhook test:"
echo "   curl -X POST '$WEBHOOK_URL' -H 'Content-Type: application/json' -d '{\"test\":\"manual\",\"timestamp\":\"$(date +%s)\"}'" 