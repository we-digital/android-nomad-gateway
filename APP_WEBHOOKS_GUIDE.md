# App Webhooks Guide

## Overview

The App Webhooks feature allows you to receive HTTP notifications when specific application events occur. This is useful for monitoring app usage, tracking SIM status changes, and integrating with external systems.

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

## Configuration

1. Open the Android Nomad Gateway app
2. Go to **Settings** (gear icon in the top right)
3. Tap **App Webhooks**
4. Configure each webhook:
   - Toggle the switch to enable/disable the webhook
   - Enter the webhook URL (must start with `http://` or `https://`)
5. Configure **Enhanced Data Options**:
   - **Device Information**: Model, manufacturer, Android version
   - **SIM Information**: Operator, country, network type, dual SIM details
   - **Network Information**: IP addresses, WiFi details, connectivity status
   - **App Configuration**: Service status, rules count, permissions
6. Tap **Save** to apply changes

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

All webhooks send a JSON payload with the following structure:

```json
{
  "event": "event_type",
  "timestamp": 1234567890000,
  "device_id": "Device Model",
  "message": "Human-readable description",
  "data": {
    "android_version": "13",
    "app_version": "1.0.0",
    "device_info": {
      // Enhanced device information (if enabled)
    }
  }
}
```

### Enhanced Manual Start Payload Example

```json
{
  "event": "app_manual_start",
  "timestamp": 1701234567890,
  "device_id": "Pixel 7",
  "message": "Application started manually by user",
  "data": {
    "android_version": "13",
    "app_version": "1.0.0",
    "device_info": {
      "device_model": "Pixel 7",
      "device_manufacturer": "Google",
      "device_brand": "google",
      "device_product": "cheetah",
      "android_version": "13",
      "android_sdk": 33,
      "device_name": "My Pixel 7",
      "sim_info": {
        "sim_state": "READY",
        "network_operator": "T-Mobile",
        "sim_operator": "T-Mobile",
        "network_country": "US",
        "sim_country": "US",
        "phone_type": "GSM",
        "network_type": "LTE",
        "sim_slots": [
          {
            "slot_index": 0,
            "subscription_id": 1,
            "display_name": "T-Mobile",
            "carrier_name": "T-Mobile",
            "country_iso": "us",
            "phone_number": "+15551234567"
          }
        ]
      },
      "network_info": {
        "is_connected": true,
        "connection_type": "WIFI",
        "connection_subtype": "",
        "is_roaming": false,
        "wifi_info": {
          "ssid": "\"MyWiFi\"",
          "bssid": "aa:bb:cc:dd:ee:ff",
          "rssi": -45,
          "link_speed": 866,
          "frequency": 5180
        },
        "ip_addresses": {
          "primary_ip": "192.168.1.100",
          "all_addresses": [
            {
              "address": "192.168.1.100",
              "interface": "wlan0",
              "is_ipv4": true,
              "is_site_local": true
            }
          ]
        }
      },
      "app_config": {
        "version_name": "1.0.0",
        "version_code": 1,
        "package_name": "tech.wdg.incomingactivitygateway",
        "service_running": true,
        "service_start_count": 5,
        "forwarding_rules_count": 3,
        "manual_start_webhook_enabled": true,
        "auto_start_webhook_enabled": true,
        "sim_status_webhook_enabled": true,
        "permissions": {
          "sms": true,
          "phone_state": true,
          "call_log": true,
          "contacts": true,
          "wifi_access": true,
          "notifications": true
        }
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

- **Device Fleet Management**: Monitor device status, connectivity, and app health
- **Network Analytics**: Track WiFi usage patterns and network performance
- **SIM Management**: Monitor SIM card status across multiple devices
- **App Usage Analytics**: Analyze user engagement and auto-start reliability
- **Alert System**: Get notified when devices go offline or encounter issues
- **Compliance Monitoring**: Track permission status and configuration changes
- **Performance Monitoring**: Monitor app performance and service reliability 