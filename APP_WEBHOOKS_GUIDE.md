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
   - Tap **Save** to apply changes

## Webhook Payload Format

All webhooks send a JSON payload with the following structure:

```json
{
  "event": "event_type",
  "timestamp": 1234567890000,
  "device_id": "Device Model",
  "message": "Human-readable description",
  "data": {
    // Additional event-specific data
  }
}
```

### Manual Start Payload Example

```json
{
  "event": "app_manual_start",
  "timestamp": 1701234567890,
  "device_id": "Pixel 7",
  "message": "Application started manually by user",
  "data": {
    "android_version": "13",
    "app_version": "1.0.0"
  }
}
```

### Auto Start Payload Example

```json
{
  "event": "app_auto_start",
  "timestamp": 1701234567890,
  "device_id": "Pixel 7",
  "message": "Application started automatically",
  "data": {
    "android_version": "13",
    "app_version": "1.0.0"
  }
}
```

### SIM Status Changed Payload Example

```json
{
  "event": "sim_status_changed",
  "timestamp": 1701234567890,
  "device_id": "Pixel 7",
  "message": "SIM status changed to: SIM_READY",
  "data": {
    "sim_status": "SIM_READY",
    "operator": "T-Mobile",
    "android_version": "13"
  }
}
```

## SIM Status Values

The following SIM status values can be reported:

- `SIM_ABSENT` - No SIM card detected
- `SIM_LOCKED` - SIM card is locked (PIN required)
- `SIM_READY` - SIM card is ready for use
- `SIM_NOT_READY` - SIM card is not ready
- `SIM_PERMANENTLY_DISABLED` - SIM card is permanently disabled
- `SIM_CARD_ERROR` - SIM card I/O error
- `SIM_RESTRICTED` - SIM card is restricted
- `SIM_LOADED` - SIM card data loaded
- `SIM_PRESENT` - SIM card is present but state unknown

## Testing Webhooks

1. Configure your webhook URLs
2. Tap **Test Webhooks** to send test payloads
3. Check your webhook endpoint to verify receipt
4. Test payloads will have event names prefixed with `test_`

## Security Considerations

- Always use HTTPS URLs for production webhooks
- Implement authentication on your webhook endpoints
- Validate the payload structure and timestamp
- Consider implementing request signing for additional security

## Troubleshooting

### Webhooks Not Firing

1. Ensure the webhook is enabled (switch is on)
2. Verify the URL is correct and accessible
3. Check that the app has internet permission
4. Review device logs for error messages

### Network Errors

- The app uses a 30-second timeout for webhook requests
- Failed webhooks are not retried automatically
- Check your server logs for incoming requests

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

## Example Webhook Server (Node.js)

```javascript
const express = require('express');
const app = express();

app.use(express.json());

app.post('/webhook', (req, res) => {
  const { event, timestamp, device_id, message, data } = req.body;
  
  console.log(`Received ${event} from ${device_id} at ${new Date(timestamp)}`);
  console.log(`Message: ${message}`);
  console.log('Data:', data);
  
  // Process the webhook...
  
  res.status(200).json({ status: 'received' });
});

app.listen(3000, () => {
  console.log('Webhook server listening on port 3000');
});
```

## Integration Ideas

- **Monitoring Dashboard**: Track app usage patterns and SIM availability
- **Alert System**: Get notified when SIM cards are removed or apps crash
- **Analytics**: Analyze user engagement and auto-start reliability
- **Device Management**: Monitor fleet device status and connectivity
- **Automation**: Trigger actions based on app or SIM events 