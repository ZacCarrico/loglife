# LogLife Setup Guide

This guide walks you through setting up LogLife from scratch, including Google Cloud configuration, building the app, and first-run setup.

## Prerequisites

- Android Studio Arctic Fox (2020.3.1) or newer
- Android device running Android 8.0+ (API 26+)
- Google account
- ~30 minutes for initial setup

## Step 1: Clone the Repository

```bash
git clone https://github.com/ZacCarrico/loglife.git
cd loglife
```

## Step 2: Google Cloud Console Setup

### 2.1 Create a Google Cloud Project

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Click the project dropdown at the top → **New Project**
3. Name it `LogLife` (or any name you prefer)
4. Click **Create**
5. Wait for the project to be created, then select it

### 2.2 Enable Required APIs

1. Go to **APIs & Services** → **Library**
2. Search for and enable each of these:
   - **Google Drive API** - Click → **Enable**
   - **Google Docs API** - Click → **Enable**

### 2.3 Configure Google Auth Platform

1. Go to **Google Auth Platform** in the sidebar
2. Navigate to **Branding**:
   - Select **External** → **Create**
   - Fill in the required fields:
     - **App name**: `LogLife`
     - **User support email**: Your email
     - **Developer contact email**: Your email
   - Click **Save and Continue**
3. Navigate to **Data Access**:
   - Click **Add or Remove Scopes**
   - Add these scopes:
     - `https://www.googleapis.com/auth/drive.readonly`
     - `https://www.googleapis.com/auth/documents`
   - Click **Update** → **Save and Continue**
4. Navigate to **Audience**:
   - Click **Add Users**
   - Add your Google account email
   - Click **Save and Continue**
5. Review and click **Back to Dashboard**

### 2.4 Create OAuth Credentials

1. Go to **Google Auth Platform** → **Clients** in the sidebar
2. Click **Create Credentials** → **OAuth client ID**
3. Select **Android** as the application type
4. Fill in:
   - **Name**: `LogLife Android`
   - **Package name**: `com.loglife.app`
   - **SHA-1 certificate fingerprint**: (see below)

#### Getting Your SHA-1 Fingerprint

For **debug builds** (development):
```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

For **release builds** (production):
```bash
keytool -list -v -keystore /path/to/your/release.keystore -alias your-alias
# Enter your keystore password when prompted
```

Copy the SHA-1 value (looks like `AA:BB:CC:DD:...`) and paste it into Google Cloud Console.

5. Click **Create**
6. You'll see a confirmation - no need to download anything for Android OAuth

## Step 3: Build the App

### 3.1 Open in Android Studio

1. Open Android Studio
2. **File** → **Open** → Select the `loglife` folder
3. Wait for Gradle sync to complete (may take a few minutes first time)

### 3.2 Build and Run

1. Connect your Android device via USB
2. Enable **USB Debugging** on your device:
   - Settings → About Phone → Tap "Build Number" 7 times
   - Settings → Developer Options → Enable USB Debugging
3. In Android Studio, select your device from the dropdown
4. Click the **Run** button (green play icon)

Or via command line:
```bash
./gradlew installDebug
adb shell am start -n com.loglife.app/.ui.MainActivity
```

## Step 4: First-Run App Setup

When you first open LogLife, you'll see a setup checklist:

### 4.1 Sign in with Google
1. Tap **Sign in with Google**
2. Select your Google account (must be one you added as a test user)
3. Grant the requested permissions (Drive read, Docs edit)

### 4.2 Select Your Log Document
1. Tap **Select Document**
2. Browse your Google Drive
3. Select an existing Google Doc, or create a new one first in Google Docs
4. The document name will appear once selected

### 4.3 Enable Accessibility Service
1. Tap **Enable Accessibility**
2. This opens Android Settings → Accessibility
3. Find **LogLife** in the list
4. Toggle it **ON**
5. Confirm the permission dialog
6. Return to LogLife app

> **Why Accessibility Service?** This is the only way Android allows apps to detect hardware button presses (like volume down) when the app isn't in the foreground.

### 4.4 Download Whisper Model
1. Tap **Download Model**
2. Wait for the download (~150MB for Base model)
3. The model enables fully offline transcription

**Model Options** (can change in Settings):
| Model | Size | Speed | Accuracy |
|-------|------|-------|----------|
| Tiny | ~75MB | Fastest | Good |
| Base | ~150MB | Fast | Better (recommended) |
| Small | ~500MB | Slower | Best |

## Step 5: Test It!

Once all four setup steps show green checkmarks:

1. Lock your phone or go to home screen
2. **Hold the volume down button** for about half a second
3. You should feel a slight vibration or see a notification
4. **Speak your note** while holding the button
5. **Release** to stop recording
6. Wait a moment for transcription and sync
7. Open your Google Doc - your note should be at the top!

## Troubleshooting

### "Google sign-in failed"
- Verify your SHA-1 fingerprint matches your build keystore
- Ensure your email is added as a test user in Google Cloud Console
- Check that both APIs (Drive and Docs) are enabled

### Volume button not working
- Verify accessibility service is enabled (shows "Enabled" in app)
- Some devices require a restart after enabling accessibility
- Check if another app is using the volume button (like a music player)

### Transcription is empty or wrong
- Ensure Whisper model downloaded completely
- Speak clearly and at normal volume
- Hold the button steady while speaking
- Try in a quieter environment

### Notes not syncing
- Check internet connection
- Verify Google account still has access
- Look at pending queue in Settings → View Pending Notes
- Try "Sync Now" button in the queue screen

### App crashes on startup
- Clear app data: Settings → Apps → LogLife → Clear Data
- Reinstall the app
- Check Android Studio logs for specific error

## Advanced Configuration

### Using a Different Whisper Model

In Settings, tap on the Model section to change models. Smaller models are faster but less accurate; larger models are more accurate but slower and use more storage.

### Custom Document Format

The default format is:
```
2025-12-22, Mon, 16:00
Your transcribed text here

[Previous entries below...]
```

To customize, modify `GoogleDocsManager.kt`:
- `formatTimestamp()` - Change date/time format
- `createNoteText()` - Change overall note structure

### Building for Release

1. Create a release keystore:
   ```bash
   keytool -genkey -v -keystore loglife-release.keystore -alias loglife -keyalg RSA -keysize 2048 -validity 10000
   ```

2. Add a new OAuth credential in Google Cloud Console with the release SHA-1

3. Build release APK:
   ```bash
   ./gradlew assembleRelease
   ```

## Getting Help

- Open an issue on [GitHub](https://github.com/ZacCarrico/loglife/issues)
- Check existing issues for similar problems
- Include Android version, device model, and error logs when reporting issues
