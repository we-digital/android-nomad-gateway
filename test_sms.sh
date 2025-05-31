#!/bin/bash
# SMS Testing Script for Android Nomad Gateway

export ANDROID_HOME=~/Library/Android/sdk
export PATH=$PATH:$ANDROID_HOME/emulator:$ANDROID_HOME/platform-tools

# Load configuration
WEBHOOK_URL="https://example.com/test_webhook"
TEST_PHONE="+1234567890"

echo "🔄 Testing SMS forwarding..."
echo "📱 Sending test SMS to emulator..."
echo "🌐 Webhook endpoint: $WEBHOOK_URL"

# Send SMS via ADB
adb emu sms send "$TEST_PHONE" "Automated test SMS message - $(date)"

if [ $? -eq 0 ]; then
    echo "✅ SMS sent successfully!"
    echo "📊 Check your webhook endpoint for the forwarded data."
    echo "🔍 You can also check logs with: adb logcat | grep 'IncomingActivityGateway'"
    
    # Wait a moment for processing
    echo "⏱️  Waiting 3 seconds for processing..."
    sleep 3
    
    # Test webhook connectivity
    echo "🧪 Testing webhook connectivity..."
    curl -s -X POST "$WEBHOOK_URL" \
        -H "Content-Type: application/json" \
        -H "User-Agent: Android-Nomad-Gateway-Test/1.0" \
        -d '{
            "from": "'"$TEST_PHONE"'",
            "text": "Test connectivity check",
            "timestamp": "'$(date +%s)'",
            "sim": "sim1",
            "type": "sms",
            "test": true
        }' && echo "✅ Webhook connectivity test completed"
else
    echo "❌ Failed to send SMS. Make sure emulator is running and ADB is connected."
fi

echo ""
echo "Expected webhook payload:"
echo "{"
echo "  \"from\": \"$TEST_PHONE\","
echo "  \"text\": \"Automated test SMS message - [timestamp]\","
echo "  \"timestamp\": \"[unix_timestamp]\","
echo "  \"sim\": \"sim1\","
echo "  \"type\": \"sms\""
echo "}" 