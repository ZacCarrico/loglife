# Agent Instructions for LogLife Development

This file contains instructions for AI agents (like Claude) working on the LogLife Android app.

## Project Overview

LogLife is a personal life-logging Android app that captures voice notes via volume button press and automatically prepends them to a Google Doc with timestamps.

**Tech Stack:**
- **Language**: Kotlin
- **Build System**: Gradle 8.2+
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 34 (Android 14)
- **Theme**: Material Design 3

## Code Structure

```
app/src/main/
├── java/com/loglife/app/
│   ├── data/              # Room database, preferences
│   ├── service/           # Background services (recording, sync)
│   ├── ui/                # Activities (MainActivity, SettingsActivity, etc.)
│   └── util/              # Utilities (audio, auth, Google Docs, Whisper)
└── res/
    ├── layout/            # XML UI layouts
    ├── values/            # colors.xml, strings.xml, themes.xml
    └── drawable/          # Graphics and icons
```

## Building and Testing on Device

### Prerequisites

1. **Android SDK**: Ensure Android SDK is installed
   - Common locations: `~/Library/Android/sdk` (macOS), `~/Android/Sdk` (Linux)
2. **Gradle**: Project uses Gradle wrapper
3. **Device**: Android device with USB debugging enabled

### First-Time Setup

1. **Generate Gradle wrapper** (if `./gradlew` doesn't work):
   ```bash
   # If you have gradle installed locally
   gradle wrapper

   # Or if gradle is in /tmp (common in some environments)
   /tmp/gradle-*/bin/gradle wrapper
   ```

2. **Configure SDK location**:
   Create `local.properties` with your Android SDK path:
   ```bash
   echo "sdk.dir=$HOME/Library/Android/sdk" > local.properties  # macOS
   # or
   echo "sdk.dir=$HOME/Android/Sdk" > local.properties  # Linux
   ```

### Build Commands

```bash
# Clean build
./gradlew clean assembleDebug

# Build only
./gradlew assembleDebug

# Build and install in one command
./gradlew installDebug
```

### Device Connection

Check connected devices:
```bash
adb devices
```

Expected output:
```
List of devices attached
<device-id>    device
```

### Install and Launch

```bash
# Install the app
./gradlew installDebug

# Launch the app
adb shell am start -n com.loglife.app/.ui.MainActivity
```

### Monitor Logs

```bash
# All app logs
adb logcat | grep -i loglife

# Activity launch logs
adb logcat -d | grep -i "MainActivity" | tail -20
```

### Clear App Data (for fresh start)

```bash
adb shell pm clear com.loglife.app
```

## UI/UX Guidelines

### Material Design 3

The app uses Material Design 3 with the following theme:
- Parent: `Theme.Material3.DayNight.NoActionBar`
- Primary color: `#6200EE` (purple)
- Background: `#FFFFFF` (white)

## Common Tasks

### Making Commits

Use descriptive commit messages:
```bash
git add .
git commit -m "Add feature X

- Brief description of what changed
- Why the change was needed
- What files were affected"
```

## Common Issues

### Issue: `gradle: not found`
**Solution**: Generate wrapper using `gradle wrapper` or if gradle is in `/tmp`:
```bash
/tmp/gradle-*/bin/gradle wrapper
```

### Issue: `SDK location not found`
**Solution**: Create `local.properties` with SDK path:
```bash
# macOS
echo "sdk.dir=$HOME/Library/Android/sdk" > local.properties

# Linux
echo "sdk.dir=$HOME/Android/Sdk" > local.properties

# Windows (Git Bash)
echo "sdk.dir=$LOCALAPPDATA/Android/Sdk" > local.properties
```

### Issue: Device not detected
**Solution**:
1. Check USB cable supports data transfer
2. Enable USB debugging in Developer Options
3. Restart adb: `adb kill-server && adb start-server`

## File Locations

- **APK Output**: `app/build/outputs/apk/debug/app-debug.apk`
- **Color Resources**: `app/src/main/res/values/colors.xml`
- **Layout Files**: `app/src/main/res/layout/`
- **Kotlin Source**: `app/src/main/java/com/loglife/app/`

## Git Workflow

The project uses feature branches. Always:
1. Create a new branch from main
2. Make changes
3. Test on device
4. Commit with descriptive messages

## Important Notes

1. **Always test on device** - Emulator behavior may differ from real hardware
2. **Keep it simple** - Don't over-engineer, focus on usability

## Resources

- Android Design Guidelines: https://developer.android.com/design
- Material Design 3: https://m3.material.io
- Project README: `README.md`

## Quick Reference

```bash
# Complete workflow
gradle wrapper                                    # Setup (if needed)
echo "sdk.dir=$HOME/Library/Android/sdk" > local.properties  # Setup (if needed)
./gradlew clean assembleDebug                     # Build
adb devices                                       # Check device
./gradlew installDebug                            # Install
adb shell am start -n com.loglife.app/.ui.MainActivity  # Launch
adb logcat | grep -i loglife                      # Monitor
```
