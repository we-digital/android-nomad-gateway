# App Webhooks Guide

## Overview

The App Webhooks feature allows you to receive HTTP notifications when specific application events occur. This includes both dedicated app webhook events and enhanced data for individual forwarding rules.

## Available Webhook Events

### 1. Application Started Manually
- **Event**: `app_manual_start`
- **Triggered**: When the user opens the app by tapping the app icon
- **Use Case**: Track when users actively engage with the app

### 2. Application Auto-Started
- **Event**: `app_auto_start`
- **Triggered**: When the app starts automatically (e.g., after device boot, service restart)
- **Use Case**: Monitor background service reliability and auto-start functionality

### 3. SIM Network Status Changed
- **Event**: `sim_status_changed`
- **Triggered**: When the SIM card status changes (inserted, removed, ready, locked, etc.)
- **Use Case**: Track SIM card availability and network connectivity

### 4. Enhanced Forwarding Rules
- **Events**: `sms_received`, `call_received`, `push_notification_received`
- **Triggered**: When SMS messages, calls, or push notifications are received (if enhanced data is enabled per rule)
- **Use Case**: Get comprehensive device and network information with each forwarded message

## Enhanced Data Configuration

The app provides two levels of enhanced data configuration:

### 1. App Webhooks (Global Settings)
Configure enhanced data for dedicated app webhook events (app start, SIM status changes) in Settings → App Webhooks.

### 2. Per-Rule Enhanced Data (Individual Forwarding Rules)
Each forwarding rule can have its own enhanced data configuration, providing granular control over what information to include with each specific webhook.

## Per-Rule Enhanced Data Configuration

When creating or editing a forwarding rule, you can configure enhanced data options specifically for that rule:

### Configuration Options

1. **Enable Enhanced Data**: Master switch to enable enhanced data for this rule
2. **Device Information**: Model, manufacturer, Android version, device name
3. **SIM Information**: Operator details, network type, dual SIM support, phone numbers
4. **Network Information**: WiFi details, IP addresses, connectivity status, roaming
5. **App Configuration**: Service status, rules count, permissions, webhook status

### Benefits of Per-Rule Configuration

- **Granular Control**: Different rules can include different types of enhanced data
- **Performance Optimization**: Only collect needed data for each specific use case
- **Privacy Control**: Sensitive information can be limited to specific rules
- **Bandwidth Efficiency**: Reduce payload size by including only relevant data
- **Use Case Optimization**: Tailor data collection to specific integration needs

## Configuration

### App Webhooks Configuration
1. Open the Android Nomad Gateway app
2. Go to **Settings** (gear icon in the top right)
3. Tap **App Webhooks**
4. Configure each webhook:
   - Toggle the switch to enable/disable the webhook
   - Enter the webhook URL (must start with `http://` or `https://`)
5. Configure **Enhanced Data Options** for app webhooks:
   - **Device Information**: Model, manufacturer, Android version
   - **SIM Information**: Operator, country, network type, dual SIM details
   - **Network Information**: IP addresses, WiFi details, connectivity status
   - **App Configuration**: Service status, rules count, permissions
6. Tap **Save** to apply changes

### Per-Rule Enhanced Data Configuration
1. Open the Android Nomad Gateway app
2. Go to **Main Screen** and tap **+** to add a new rule or edit an existing rule
3. Configure basic rule settings (activity type, sources, webhook URL)
4. In the **Enhanced Data Configuration** section:
   - Toggle **Enable Enhanced Data** to activate enhanced data for this rule
   - Select which types of enhanced data to include:
     - **Device Information**: Basic device details
     - **SIM Information**: SIM and network details
     - **Network Information**: Connectivity and WiFi details
     - **App Configuration**: App status and configuration
5. Configure JSON template and other settings as needed
6. Tap **Save** to create/update the rule

## Enhanced Data Features

The app now includes comprehensive device information in webhook payloads:

### Device Information
- Device model, manufacturer, brand
- Android version and SDK level
- Device name (from system settings)

### SIM Information
- SIM state and operator details
- Network country and type
- Dual SIM support with slot information
- Phone numbers (if available)

### Network Information
- Connection status and type
- WiFi details (SSID, signal strength, frequency)
- IP addresses (IPv4/IPv6)
- Roaming status

### App Configuration
- Service running status and start count
- Number of forwarding rules configured
- Webhook configuration status
- Permission status for all app permissions

## Required Permissions

The enhanced webhook features require the following permissions:

- **SMS Access** - To receive and forward SMS messages
- **Phone State** - To identify SIM cards and monitor calls
- **Call Log** - To detect incoming calls
- **Contacts** - To resolve phone numbers to names
- **Phone Numbers** - To identify your phone numbers
- **WiFi Access** - To collect network information for webhooks
- **Notifications** - To show service status (Android 13+)
- **Foreground Service** - To run reliably in background (Android 14+)

## Webhook Payload Format

### App Webhook Events
App webhook events (app start, SIM status) use the global enhanced data configuration:

```json
{
  "event": "app_manual_start|app_auto_start|sim_status_changed",
  "timestamp": 1234567890000,
  "device_id": "Device Model",
  "message": "Human-readable description",
  "data": {
    "android_version": "13",
    "app_version": "1.0.0",
    "device_info": {
      // Enhanced device information (if enabled globally)
    }
  }
}
```

### Enhanced Forwarding Rule Events
Forwarding rule events use per-rule enhanced data configuration:

```json
{
  "event": "sms_received|call_received|push_notification_received",
  "timestamp": 1234567890000,
  "device_id": "Device Model",
  "message": "Human-readable description",
  "data": {
    // Event-specific data (from, text, contact, etc.)
    "device_info": {
      // Enhanced device information (if enabled for this rule)
    }
  }
}
```

### Enhanced SMS Payload Example (Per-Rule Configuration)

```json
{
  "event": "sms_received",
  "timestamp": 1701234567890,
  "device_id": "Pixel 7",
  "message": "SMS received from +15551234567",
  "data": {
    "from": "+15551234567",
    "text": "Hello, this is a test message",
    "sim": "T-Mobile",
    "sentStamp": 1701234567890,
    "receivedStamp": 1701234567895,
    "device_info": {
      "device_model": "Pixel 7",
      "device_manufacturer": "Google",
      "sim_info": {
        "sim_state": "READY",
        "network_operator": "T-Mobile",
        "sim_operator": "T-Mobile"
      },
      "network_info": {
        "is_connected": true,
        "connection_type": "WIFI",
        "wifi_info": {
          "ssid": "\"MyWiFi\"",
          "rssi": -45
        }
      },
      "app_config": {
        "version_name": "1.0.0",
        "service_running": true,
        "forwarding_rules_count": 3
      }
    }
  }
}
```

### Enhanced Call Payload Example (Per-Rule Configuration)

```json
{
  "event": "call_received",
  "timestamp": 1701234567890,
  "device_id": "Pixel 7",
  "message": "Incoming call from +15551234567",
  "data": {
    "from": "+15551234567",
    "contact": "John Doe",
    "timestamp": 1701234567890,
    "duration": 0,
    "sim": "T-Mobile",
    "sentStamp": 1701234567890,
    "receivedStamp": 1701234567895,
    "device_info": {
      "device_model": "Pixel 7",
      "device_manufacturer": "Google",
      "sim_info": {
        "sim_state": "READY",
        "network_operator": "T-Mobile"
      },
      "network_info": {
        "is_connected": true,
        "connection_type": "MOBILE"
      },
      "app_config": {
        "version_name": "1.0.0",
        "service_running": true
      }
    }
  }
}
```

### Enhanced Push Notification Payload Example (Per-Rule Configuration)

```json
{
  "event": "push_notification_received",
  "timestamp": 1701234567890,
  "device_id": "Pixel 7",
  "message": "Push notification received from com.whatsapp",
  "data": {
    "from": "com.whatsapp",
    "package": "com.whatsapp",
    "title": "New Message",
    "content": "You have a new message",
    "text": "New Message: You have a new message",
    "sim": "notification",
    "sentStamp": 1701234567890,
    "receivedStamp": 1701234567895,
    "device_info": {
      "device_model": "Pixel 7",
      "device_manufacturer": "Google",
      "network_info": {
        "is_connected": true,
        "connection_type": "WIFI"
      },
      "app_config": {
        "version_name": "1.0.0",
        "service_running": true,
        "forwarding_rules_count": 5
      }
    }
  }
}
```

## SIM Status Values

The following SIM status values can be reported:

- `ABSENT` - No SIM card detected
- `CARD_IO_ERROR` - SIM card I/O error
- `CARD_RESTRICTED` - SIM card is restricted
- `NETWORK_LOCKED` - SIM card is network locked
- `NOT_READY` - SIM card is not ready
- `PERM_DISABLED` - SIM card is permanently disabled
- `PIN_REQUIRED` - SIM card PIN required
- `PUK_REQUIRED` - SIM card PUK required
- `READY` - SIM card is ready for use
- `UNKNOWN` - SIM card state unknown

## Network Types

Common network types include:

- `LTE` - 4G LTE network
- `UMTS` - 3G UMTS network
- `GSM` - 2G GSM network
- `CDMA` - CDMA network
- `EDGE` - Enhanced Data rates for GSM Evolution
- `GPRS` - General Packet Radio Service
- `HSPA` - High Speed Packet Access
- `WIFI` - WiFi connection

## Testing Webhooks

1. Configure your webhook URLs
2. Enable desired enhanced data options
3. Tap **Test Webhooks** to send test payloads
4. Check your webhook endpoint to verify receipt
5. Test payloads will have event names prefixed with `test_`

## Privacy and Security Considerations

- **Enhanced data is optional**: You can disable any enhanced data collection
- **Local processing**: All data is collected locally and only sent to your configured webhooks
- **Permission-based**: Only data accessible with granted permissions is collected
- **No external services**: No data is sent to third-party analytics or tracking services
- **User control**: You have complete control over what data is included

### Security Best Practices

- Always use HTTPS URLs for production webhooks
- Implement authentication on your webhook endpoints
- Validate the payload structure and timestamp
- Consider implementing request signing for additional security
- Regularly review and audit webhook configurations

## Troubleshooting

### Webhooks Not Firing

1. Ensure the webhook is enabled (switch is on)
2. Verify the URL is correct and accessible
3. Check that the app has internet permission
4. Review device logs for error messages

### Missing Enhanced Data

1. Check that enhanced data options are enabled
2. Verify required permissions are granted
3. Some data may not be available on all devices
4. Check the Settings page for permission status

### Network Errors

- The app uses a 30-second timeout for webhook requests
- Failed webhooks are not retried automatically
- Check your server logs for incoming requests
- Verify SSL certificates for HTTPS endpoints

### SIM Status Not Updating

- Ensure the app has Phone State permission
- Some devices may not report all SIM state changes
- Try removing and reinserting the SIM card to test

## Best Practices

1. **Use HTTPS**: Always use secure connections for webhooks
2. **Handle Failures**: Your webhook endpoint should return 2xx status codes
3. **Process Asynchronously**: Don't block on webhook processing
4. **Monitor Reliability**: Track webhook delivery success rates
5. **Validate Timestamps**: Check that timestamps are recent to prevent replay attacks
6. **Selective Data**: Only enable enhanced data options you actually need
7. **Regular Audits**: Periodically review webhook configurations and data usage

## Example Webhook Server (Node.js)

```javascript
const express = require('express');
const app = express();

app.use(express.json());

app.post('/webhook', (req, res) => {
  const { event, timestamp, device_id, message, data } = req.body;
  
  console.log(`Received ${event} from ${device_id} at ${new Date(timestamp)}`);
  console.log(`Message: ${message}`);
  
  // Handle different event types
  switch(event) {
    case 'sms_received':
      console.log(`SMS from ${data.from}: ${data.text}`);
      break;
    case 'call_received':
      console.log(`Call from ${data.from} (${data.contact})`);
      break;
    case 'push_notification_received':
      console.log(`Notification from ${data.package}: ${data.title}`);
      break;
    case 'app_manual_start':
    case 'app_auto_start':
      console.log(`App started: ${event}`);
      break;
    case 'sim_status_changed':
      console.log(`SIM status: ${data.sim_status}`);
      break;
  }
  
  // Process enhanced device info if available
  if (data.device_info) {
    const deviceInfo = data.device_info;
    console.log(`Device: ${deviceInfo.device_manufacturer} ${deviceInfo.device_model}`);
    
    if (deviceInfo.sim_info) {
      console.log(`SIM: ${deviceInfo.sim_info.sim_operator} (${deviceInfo.sim_info.sim_state})`);
    }
    
    if (deviceInfo.network_info) {
      console.log(`Network: ${deviceInfo.network_info.connection_type}`);
      if (deviceInfo.network_info.wifi_info) {
        console.log(`WiFi: ${deviceInfo.network_info.wifi_info.ssid}`);
      }
    }
    
    if (deviceInfo.app_config) {
      console.log(`App: v${deviceInfo.app_config.version_name} (${deviceInfo.app_config.forwarding_rules_count} rules)`);
    }
  }
  
  res.status(200).json({ status: 'received' });
});

app.listen(3000, () => {
  console.log('Webhook server listening on port 3000');
});
```

## Integration Ideas

- **Unified Device Monitoring**: Monitor all device activity (SMS, calls, notifications, app events) in one place
- **Enhanced Analytics**: Analyze communication patterns with rich device context
- **Network Performance Tracking**: Monitor WiFi usage and network performance across all events
- **Security Monitoring**: Track device access patterns and detect anomalies
- **Fleet Management**: Comprehensive device status monitoring for enterprise deployments
- **Compliance Reporting**: Detailed audit trails with device and network context
- **Performance Optimization**: Identify patterns affecting app performance and reliability
- **User Behavior Analysis**: Understand how users interact with the device and apps 