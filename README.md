# LogLife

A personal life-logging Android app that captures voice notes via volume button press and automatically prepends them to a Google Doc with timestamps. Built for frictionless memory capture throughout your day.

## Why LogLife?

Capturing thoughts, observations, and memories shouldn't require unlocking your phone, opening an app, and navigating menus. LogLife lets you **hold the volume button, speak, and release** - your words are transcribed on-device and added to your personal log document automatically.

Perfect for:
- 📝 Daily journaling and reflection
- 💡 Capturing fleeting ideas before they disappear  
- 🧠 Memory support and cognitive health tracking
- 📊 Building a searchable archive of your thoughts

## Features

- **Volume Button Activation**: Hold the volume down button to record (requires Accessibility Service)
- **On-Device Transcription**: Uses Whisper.cpp for fully offline, private speech-to-text
- **Google Docs Integration**: Automatically prepends transcriptions to your selected Google Doc
- **Timestamp Format**: `2025-12-22, Mon, 16:00` followed by the transcription
- **Offline Queue**: Notes are queued locally when offline and synced when connectivity returns
- **Secure Storage**: All credentials stored using Android's EncryptedSharedPreferences
- **Privacy First**: Audio is processed locally and deleted after transcription

## Setup Instructions

### 1. Google Cloud Console Setup

Before the app can access Google Drive and Docs, you need to create OAuth credentials:

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select an existing one
3. Enable the following APIs:
   - Google Drive API
   - Google Docs API
4. Go to **APIs & Services > Credentials**
5. Click **Create Credentials > OAuth client ID**
6. Select **Android** as the application type
7. Enter your app's package name: `com.loglife.app`
8. Get your SHA-1 fingerprint:
   ```bash
   # For debug keystore
   keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
   
   # Or for release keystore
   keytool -list -v -keystore your-release-key.keystore -alias your-alias
   ```
9. Enter the SHA-1 fingerprint
10. Click **Create**

### 2. Configure OAuth Consent Screen

1. Go to **APIs & Services > OAuth consent screen**
2. Choose **External** (or Internal if you have a Workspace account)
3. Fill in the required fields:
   - App name: LogLife
   - User support email: your email
   - Developer contact: your email
4. Add scopes:
   - `https://www.googleapis.com/auth/drive.readonly`
   - `https://www.googleapis.com/auth/documents`
5. Add test users (your Google account) if in testing mode

### 3. Build the App

1. Open the project in Android Studio (Arctic Fox or newer)
2. Let Gradle sync complete
3. Build the project: **Build > Make Project**

### 4. Install on Device

1. Connect your Pixel 7 via USB with USB debugging enabled
2. Run the app from Android Studio, or:
   ```bash
   ./gradlew installDebug
   adb shell am start -n com.loglife.app/.ui.MainActivity
   ```

### 5. App Setup

1. **Sign in with Google**: Tap the sign-in button and authorize the app
2. **Select a Google Doc**: Browse your Drive and select the document for notes
3. **Enable Accessibility Service**:
   - Tap "Enable Accessibility"
   - Find "LogLife" in the list
   - Toggle it ON
   - Confirm the permission dialog
4. **Download Whisper Model**: Choose a model size and download it

### 6. Usage

Once setup is complete:
1. Hold the **volume down button** for ~300ms to start recording
2. Speak your note
3. Release the button to stop recording
4. The app will transcribe and add it to your Google Doc

## Whisper Model Options

| Model | Size | Speed | Accuracy |
|-------|------|-------|----------|
| Tiny | ~75MB | Fastest | Good |
| Base | ~150MB | Fast | Better |
| Small | ~500MB | Moderate | Best |

For Pixel 7, the **Base** model is recommended as a good balance.

## Technical Notes

### Whisper.cpp Integration

The current implementation includes a placeholder for Whisper.cpp JNI integration. To complete the setup:

1. Clone [whisper.cpp](https://github.com/ggerganov/whisper.cpp)
2. Build for Android:
   ```bash
   cd whisper.cpp
   mkdir build-android && cd build-android
   cmake -DCMAKE_TOOLCHAIN_FILE=$NDK/build/cmake/android.toolchain.cmake \
         -DANDROID_ABI=arm64-v8a \
         -DANDROID_PLATFORM=android-26 ..
   make
   ```
3. Copy the resulting `.so` files to `app/src/main/jniLibs/arm64-v8a/`
4. Create JNI bindings in `app/src/main/cpp/`

Alternatively, use a pre-built library like:
- [whisper-jni-android](https://github.com/nicksinger/whisper-jni-android)
- [whisper.cpp Android examples](https://github.com/ggerganov/whisper.cpp/tree/master/examples/whisper.android)

### Accessibility Service

The accessibility service is required to intercept volume button presses system-wide. This is the only way for Android apps to detect hardware buttons outside the app's foreground.

Users must manually enable this in Settings > Accessibility > LogLife.

### Offline Support

When offline:
1. Recording and transcription work normally (on-device)
2. Notes are saved to local Room database
3. WorkManager schedules sync when connectivity returns
4. Notes sync in chronological order

## Project Structure

```
app/src/main/
├── java/com/loglife/app/
│   ├── LogLifeApp.kt          # Application class
│   ├── data/
│   │   ├── AppDatabase.kt        # Room database
│   │   └── PreferencesManager.kt # Secure preferences
│   ├── service/
│   │   ├── VolumeButtonAccessibilityService.kt
│   │   ├── RecordingService.kt
│   │   ├── SyncWorker.kt
│   │   └── BootReceiver.kt
│   ├── ui/
│   │   ├── MainActivity.kt
│   │   ├── SettingsActivity.kt
│   │   ├── DrivePickerActivity.kt
│   │   └── QueueActivity.kt
│   └── util/
│       ├── AudioRecorder.kt
│       ├── GoogleAuthManager.kt
│       ├── GoogleDocsManager.kt
│       ├── WhisperManager.kt
│       └── NetworkUtils.kt
├── res/
│   ├── layout/
│   ├── values/
│   ├── drawable/
│   └── xml/
└── AndroidManifest.xml
```

## Permissions

| Permission | Purpose |
|------------|---------|
| RECORD_AUDIO | Voice recording |
| INTERNET | Google API access |
| ACCESS_NETWORK_STATE | Check connectivity |
| FOREGROUND_SERVICE | Background recording |
| POST_NOTIFICATIONS | Status notifications |
| BIND_ACCESSIBILITY_SERVICE | Volume button detection |

## Troubleshooting

### "Google sign-in failed"
- Verify SHA-1 fingerprint matches your keystore
- Check that OAuth consent screen is configured
- Ensure your account is added as a test user

### Volume button not working
- Verify accessibility service is enabled in Settings
- Some devices may require a restart after enabling
- Check that no other app is using the accessibility service for volume

### Transcription fails
- Ensure Whisper model is fully downloaded
- Check available storage space
- Try a smaller model if device is slow

### Notes not syncing
- Verify internet connection
- Check that Google account still has access
- Look at pending queue in Settings

## License

MIT License - feel free to modify and distribute.

## Acknowledgments

- [Whisper.cpp](https://github.com/ggerganov/whisper.cpp) - On-device speech recognition
- [Google APIs](https://developers.google.com/) - Drive and Docs integration
