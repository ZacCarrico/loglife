# Development Notes

## AI Hallucination Issues (Dec 31, 2025)

### Whisper Library Dependency
**Problem:** The initial project setup included a hallucinated dependency:
```gradle
implementation("com.github.nicksinger:whisper-jni-android:1.0.0")
```

**Issue:** This library does not exist. JitPack returns 401 Unauthorized, and there's no GitHub repository at `github.com/nicksinger/whisper-jni-android`.

**Alternatives Evaluated:**

1. **GiviMAD/whisper-jni** (⭐ 148)
   - Maven: `io.github.givimad:whisper-jni:1.7.1`
   - Pros: Easy Gradle dependency, pre-built binaries
   - Cons: Wrapper layer, may lag behind official updates

2. **Official whisper.cpp** (⭐ 45.4k) - **RECOMMENDED**
   - Location: `examples/whisper.android` in [ggml-org/whisper.cpp](https://github.com/ggml-org/whisper.cpp)
   - Pros: Official, best performance, latest features
   - Cons: Requires NDK build setup
   - **Status:** Testing in `feature/official-whisper` branch

3. **argmaxinc/WhisperKitAndroid** (⭐ 195)
   - Maven: `com.argmaxinc:whisperkit:0.3.3`
   - Pros: Hardware acceleration (NPU/GPU)
   - Cons: Experimental API

4. **vilassn/whisper_android** (⭐ 592)
   - Demo project using TensorFlow Lite
   - Not production-ready

**Decision:** Integrating official whisper.cpp for best long-term support and performance.

### Google Docs API Dependency
**Problem:** Initial version specified non-existent version:
```gradle
implementation("com.google.apis:google-api-services-docs:v1-rev20231115-2.0.0")
```

**Fix:** Updated to latest working version:
```gradle
implementation("com.google.apis:google-api-services-docs:v1-rev20220609-1.32.1")
```

## MVP Pivot (Dec 31, 2025)

### Original Vision
Voice-to-Google-Docs life-logging with on-device Whisper transcription, volume button trigger, offline queue.

### New Simplified MVP
After discovering Whisper dependency was hallucinated, pivoting to minimal viable product:

**MVP Features (Direct Sync Only):**
1. Large, scrollable text box (Material Design)
2. Google Docs integration - remember last used doc
3. "Log" button - **prepend** text to doc with timestamp (newest at top)
4. Requires internet connection
5. Keyboard input only

**Rationale:**
- Removes ALL complexity: no audio, no offline, no background services
- Validates core value proposition: easy life logging workflow
- Fast to build and test

### Future Features (Post-MVP)
- 📋 Offline queue (Room + WorkManager)
- 🎤 Voice input (Whisper integration)
- 🔘 Volume button trigger (Accessibility Service)
- 🔄 Background sync
- 📱 Boot receiver

### Removed from MVP
- ❌ All Whisper/audio code
- ❌ Room database
- ❌ WorkManager
- ❌ RecordingService
- ❌ Volume button service
- ❌ Boot receiver
- ❌ Offline queue

### Keeping for MVP
- ✅ Google Drive & Docs API
- ✅ OAuth authentication
- ✅ Simple UI (MainActivity, SettingsActivity, DrivePickerActivity)
- ✅ Network utilities
- ✅ Preferences (last used doc)

## Branch Strategy
- `main` - Original full-featured design (build broken)
- `feature/official-whisper` - [ABANDONED] Whisper exploration
- `mvp-simplified` - New minimal MVP (text-only, direct sync)
