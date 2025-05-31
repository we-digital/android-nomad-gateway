# Incoming SMS, Calls & Notifications Gateway

This is a free, open-source Android app that automatically forwards incoming SMS messages, phone calls, and push notifications to specified URLs as JSON via HTTP POST.

## Features

* **SMS Forwarding**: Forward SMS from specific numbers or all senders
* **Call Monitoring**: Forward incoming call information with caller details and contact names
* **Push Notification Forwarding**: Forward notifications from specific apps
* **Flexible Filtering**: Configure rules for specific phone numbers, apps, or monitor all sources
* **Modern UI**: Material Design 3 interface with structured configuration
* **Template Customization**: Build custom JSON payloads with template variables
* **HTTP Headers**: Configure custom headers for webhook requests
* **Retry Logic**: Failed requests retry with exponential backoff
* **SSL Options**: Option to ignore SSL certificate errors
* **Built-in Testing**: Test webhook configurations before saving
* **No Cloud Dependencies**: All processing happens locally on your device

## Activity Types

### SMS Messages
Forward incoming text messages with sender information, message content, timestamps, and SIM slot details.

**Available template variables:**
- `%from%` - Sender phone number
- `%text%` - Message content
- `%sentStamp%` - Message sent timestamp
- `%receivedStamp%` - Message received timestamp
- `%sim%` - SIM slot identifier

### Phone Calls
Monitor incoming calls and forward caller information including contact names when available.

**Available template variables:**
- `%from%` - Caller phone number
- `%contact%` - Contact name (if available in contacts)
- `%timestamp%` - Call timestamp
- `%duration%` - Call duration (0 for incoming calls)

### Push Notifications
Forward notifications from specific apps with title, content, and app information.

**Available template variables:**
- `%package%` - App package name
- `%title%` - Notification title
- `%content%` - Notification content
- `%text%` - Combined notification text
- `%sentStamp%` - Notification timestamp

## Download apk

Download apk from [release page](https://github.com/bogkonstantin/android_income_sms_gateway_webhook/releases)

Or download it from F-Droid

[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
     alt="Get it on F-Droid"
     height="80">](https://f-droid.org/packages/tech.bogomolov.incomingsmsgateway/)

## Development & Release Management

This project includes automated version management and release scripts for streamlined development:

### Quick Release
```bash
# Complete release workflow (recommended)
./release.sh minor "Add new features and improvements"

# Version bump only
./version_bump.sh patch "Fix critical bug"
```

### Available Scripts
- **`version_bump.sh`** - Automated version bumping with git commits and tags
- **`release.sh`** - Complete release workflow with APK building and GitHub releases
- **Semantic versioning** support (major/minor/patch)
- **Automatic changelog** generation
- **Build verification** and rollback capabilities

For detailed documentation, see [VERSION_MANAGEMENT.md](VERSION_MANAGEMENT.md).

### Building from Source
```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease
```

## How to use

Set up App Permissions for you phone after installation. For example, enable "Autostart" if needed
and "Display pop-up windows while running in the background" from Xiaomi devices.

Set sender phone number or name and URL. It should match the number or name you see in the SMS messenger app. 
If you want to send any SMS to URL, use * (asterisk symbol) as a name.  

Every incoming SMS will be sent immediately to the provided URL.
If the response code is not 2XX or the request ended with a connection error, the app will try to
send again up to 10 times (can be changed in parameters).
Minimum first retry will be after 10 seconds, later wait time will increase exponentially.
If the phone is not connected to the internet, the app will wait for the connection before the next
attempt.  

If at least one Forwarding config is created and all needed permissions granted - you should see F
icon in the status bar, means the app is listening for the SMS.

Press the Test button to make a test request to the server.

Press the Syslog button to view errors stored in the Logcat.

### Request info
HTTP method: POST  
Content-type: application/json; charset=utf-8  

Sample payload:  
```json
{
     "from": "%from%",
     "text": "%text%",
     "sentStamp": "%sentStamp%",
     "receivedStamp": "%receivedStamp%",
     "sim": "%sim%"
}
```

Available placeholders:
%from%
%text%
%sentStamp%
%receivedStamp%
%sim%

### Request example
Use this curl sample request to prepare your backend code
```bash
curl -X 'POST' 'https://yourwebsite.com/path' \
     -H 'content-type: application/json; charset=utf-8' \
     -d $'{"from":"1234567890","text":"Test"}'
```

### Send SMS to the Telegram

1. Create Telegram bot and channel to receive messages. [There](https://bogomolov.tech/Telegram-notification-on-SSH-login/) is short tutorial how to do that.  
2. Add new forwarding configuration in the app using this parameters:
   1. Any sender you need, * - on the screenshot
   2. Webhook URL - `https://api.telegram.org/bot<YourBOTToken>/sendMessage?chat_id=<channel_id>` - change URL using your token and channel id
   3. Use this payload as a sample `{"text":"sms from %from% with text: \"%text%\" sent at %sentStamp%"}`
   4. Save configuration

<img alt="Incoming SMS Webhook Gateway screenshot Telegram example" src="https://raw.githubusercontent.com/bogkonstantin/android_income_sms_gateway_webhook/master/fastlane/metadata/android/en-US/images/phoneScreenshots/telegram.png" width="30%"/> 

### Process Payload in PHP scripts

Since $_POST is an array from the url-econded payload, you need to get the raw payload. To do so use file_get_contents:
```php
$payload = file_get_contents('php://input');
$decoded = json_decode($payload, true);
```

### Screenshots
<img alt="Incoming SMS Webhook Gateway screenshot 1" src="https://raw.githubusercontent.com/bogkonstantin/android_income_sms_gateway_webhook/master/fastlane/metadata/android/en-US/images/phoneScreenshots/1.png" width="30%"/> <img alt="Incoming SMS Webhook Gateway screenshot 2" src="https://raw.githubusercontent.com/bogkonstantin/android_income_sms_gateway_webhook/master/fastlane/metadata/android/en-US/images/phoneScreenshots/2.png" width="30%"/> <img alt="Incoming SMS Webhook Gateway screenshot 3" src="https://raw.githubusercontent.com/bogkonstantin/android_income_sms_gateway_webhook/master/fastlane/metadata/android/en-US/images/phoneScreenshots/3.png" width="30%"/>

### Misc

This repository contains a stable app with minimum functionality. It is not archived, but not actively developing. If you need an app with merged PRs - try [this fork](https://github.com/scottmconway/android_income_sms_gateway_webhook)
