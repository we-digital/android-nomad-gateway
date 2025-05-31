#!/bin/bash
# Call Testing Script for Android Nomad Gateway

export ANDROID_HOME=~/Library/Android/sdk
export PATH=$PATH:$ANDROID_HOME/emulator:$ANDROID_HOME/platform-tools

# Load configuration
WEBHOOK_URL="https://example.com/test_webhook"
TEST_PHONE="+1987654321"

echo "🔄 Testing call forwarding..."
echo "📞 Simulating incoming call to emulator..."
echo "🌐 Webhook endpoint: $WEBHOOK_URL"

# Simulate incoming call
echo "📱 Initiating call from $TEST_PHONE..."
adb emu gsm call "$TEST_PHONE"

if [ $? -eq 0 ]; then
    echo "✅ Incoming call initiated!"
    echo "⏱️  Letting call ring for 8 seconds to trigger call detection..."
    sleep 8
    
    echo "📞 Ending call..."
    adb emu gsm cancel "$TEST_PHONE"
    
    echo "✅ Call simulation complete!"
    echo "📊 Check your webhook endpoint for the forwarded data."
    echo "🔍 You can also check logs with: adb logcat | grep 'IncomingActivityGateway'"
    
    # Wait for processing
    echo "⏱️  Waiting 3 seconds for processing..."
    sleep 3
    
    # Test webhook connectivity
    echo "🧪 Testing webhook connectivity..."
    curl -s -X POST "$WEBHOOK_URL" \
        -H "Content-Type: application/json" \
        -H "User-Agent: Android-Nomad-Gateway-Test/1.0" \
        -d '{
            "from": "'"$TEST_PHONE"'",
            "contact": "Unknown",
            "timestamp": "'$(date +%s)'",
            "duration": "8",
            "type": "call",
            "test": true
        }' && echo "✅ Webhook connectivity test completed"
        
    # Additional call state verification
    echo "🔍 Checking call state in logs..."
    adb logcat -d | grep -i "phone\|call\|telephony" | tail -5
else
    echo "❌ Failed to simulate call. Make sure emulator is running and ADB is connected."
fi

echo ""
echo "Expected webhook payload:"
echo "{"
echo "  \"from\": \"$TEST_PHONE\","
echo "  \"contact\": \"Unknown\","
echo "  \"timestamp\": \"[unix_timestamp]\","
echo "  \"duration\": \"[call_duration]\","
echo "  \"type\": \"call\""
echo "}"

echo ""
echo "💡 Note: Call detection requires:"
echo "   1. Phone permissions granted to the app"
echo "   2. CallBroadcastReceiver properly registered"
echo "   3. Phone state changes to be monitored" 