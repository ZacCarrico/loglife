# Add project specific ProGuard rules here.
# Keep Whisper JNI
-keep class com.whisperjni.** { *; }

# Keep Google API classes
-keep class com.google.api.** { *; }
-keep class com.google.apis.** { *; }

# Keep Room entities
-keep class com.voicetodoc.app.data.** { *; }
