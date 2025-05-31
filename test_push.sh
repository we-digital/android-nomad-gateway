#!/bin/bash
# Push Notification Testing Script for Android Nomad Gateway

export ANDROID_HOME=~/Library/Android/sdk
export PATH=$PATH:$ANDROID_HOME/emulator:$ANDROID_HOME/platform-tools

# Load configuration
WEBHOOK_URL="https://example.com/test_webhook"
TEST_PACKAGE="com.android.chrome"

echo "🔄 Testing push notification forwarding..."
echo "🔔 Creating test notification..."
echo "🌐 Webhook endpoint: $WEBHOOK_URL"

# Check if notification access is enabled
echo "🔍 Checking notification access..."
NOTIFICATION_ACCESS=$(adb shell settings get secure enabled_notification_listeners)
if [[ "$NOTIFICATION_ACCESS" == *"tech.wdg.incomingactivitygateway"* ]]; then
    echo "✅ Notification access is enabled"
else
    echo "⚠️  Notification access may not be enabled. Please enable it in Settings."
fi

# Method 1: Using notification command (Android 7+)
echo "📱 Method 1: Using notification command..."
adb shell cmd notification post -S bigtext -t "Test Notification" "TestTag" "This is a test notification from automated script - $(date)"

if [ $? -eq 0 ]; then
    echo "✅ Notification posted successfully!"
else
    echo "⚠️  Direct notification failed, trying alternative methods..."
fi

# Method 2: Install and use a test app to generate notifications
echo "📱 Method 2: Triggering Chrome notification..."
adb shell am start -n com.android.chrome/com.google.android.apps.chrome.Main
sleep 2

# Method 3: Using am broadcast to simulate notification
echo "📱 Method 3: Broadcasting notification intent..."
adb shell am broadcast -a android.intent.action.MAIN \
    --es title "Test Push Notification" \
    --es text "Test notification from script - $(date)" \
    --es package "$TEST_PACKAGE"

# Method 4: Create a test notification using service call
echo "📱 Method 4: Using service call..."
adb shell service call notification 1 s16 "$TEST_PACKAGE" i32 12345 s16 "Test" s16 "Test notification content" i32 0

echo "📊 Check your webhook endpoint for the forwarded data."
echo "🔍 You can also check logs with: adb logcat | grep 'IncomingActivityGateway'"

# Wait for processing
echo "⏱️  Waiting 5 seconds for processing..."
sleep 5

# Test webhook connectivity
echo "🧪 Testing webhook connectivity..."
curl -s -X POST "$WEBHOOK_URL" \
    -H "Content-Type: application/json" \
    -H "User-Agent: Android-Nomad-Gateway-Test/1.0" \
    -d '{
        "app": "'"$TEST_PACKAGE"'",
        "title": "Test Push Notification",
        "content": "Test notification from script",
        "message": "Test notification content",
        "timestamp": "'$(date +%s)'",
        "type": "push",
        "test": true
    }' && echo "✅ Webhook connectivity test completed"

# Check notification listener service
echo "🔍 Checking notification listener service..."
adb shell dumpsys notification | grep -A 5 -B 5 "tech.wdg.incomingactivitygateway"

echo ""
echo "💡 Note: For push notifications to work properly:"
echo "   1. Enable notification access for the app in Settings > Apps > Special access > Notification access"
echo "   2. Install apps that send notifications (Chrome, Gmail, etc.)"
echo "   3. Make sure NotificationListenerService is running"
echo "   4. Grant notification permissions to the app"

echo ""
echo "🔧 To enable notification access manually:"
echo "   adb shell settings put secure enabled_notification_listeners tech.wdg.incomingactivitygateway/.NotificationListener"

echo ""
echo "Expected webhook payload:"
echo "{"
echo "  \"app\": \"$TEST_PACKAGE\","
echo "  \"title\": \"Test Push Notification\","
echo "  \"content\": \"Test notification from script\","
echo "  \"message\": \"[notification_text]\","
echo "  \"timestamp\": \"[unix_timestamp]\","
echo "  \"type\": \"push\""
echo "}" 