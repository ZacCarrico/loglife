# Google OAuth Setup for LogLife

## Overview
LogLife needs Google OAuth credentials to access Google Drive and Docs APIs. Follow these steps to set it up.

## Your Debug SHA-1 Fingerprint
```
C1:6C:1E:82:53:17:B0:C9:0A:93:C2:EB:5A:7E:79:C0:78:EC:DC:AF
```
*(You'll need this in step 4)*

---

## Setup Steps

### 1. Create Google Cloud Project
1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Click **"Select a project"** → **"New Project"**
3. Name it: **"LogLife"** (or any name you prefer)
4. Click **"Create"**
5. Wait for the project to be created, then select it

### 2. Enable Required APIs
1. In your project, go to **"APIs & Services"** → **"Library"**
2. Search for and enable these APIs:
   - **Google Drive API** - Click "Enable"
   - **Google Docs API** - Click "Enable"

### 3. Configure OAuth Consent Screen
1. Go to **"APIs & Services"** → **"OAuth consent screen"**
2. Select **"External"** (unless you have a Google Workspace)
3. Click **"Create"**
4. Fill in the required fields:
   - **App name**: `LogLife`
   - **User support email**: Your email
   - **Developer contact email**: Your email
5. Click **"Save and Continue"**
6. **Scopes**: Click **"Add or Remove Scopes"**
   - Search and add: `https://www.googleapis.com/auth/drive.readonly`
   - Search and add: `https://www.googleapis.com/auth/documents`
   - Click **"Update"** → **"Save and Continue"**
7. **Test users**: Click **"Add Users"**
   - Add your Google email address (the one you'll use to test)
   - Click **"Add"** → **"Save and Continue"**
8. Review and click **"Back to Dashboard"**

### 4. Create OAuth 2.0 Credentials
1. Go to **"APIs & Services"** → **"Credentials"**
2. Click **"Create Credentials"** → **"OAuth client ID"**
3. Select **"Android"** as application type
4. Fill in:
   - **Name**: `LogLife Android App`
   - **Package name**: `com.loglife.app`
   - **SHA-1 certificate fingerprint**: Paste this:
     ```
     C1:6C:1E:82:53:17:B0:C9:0A:93:C2:EB:5A:7E:79:C0:78:EC:DC:AF
     ```
5. Click **"Create"**
6. **IMPORTANT**: You don't need to download anything - Android apps use the package name + SHA-1 for auth

### 5. (Optional) Create Web Client ID for Testing
*Only if you want to test in an emulator or need additional features*

1. In **"Credentials"**, click **"Create Credentials"** → **"OAuth client ID"**
2. Select **"Web application"**
3. Name it: `LogLife Web Client (for Android)`
4. Click **"Create"**
5. **Copy the Client ID** - you'll need it if the Android credentials don't work

---

## Verification & Testing

### Testing with Test Users (Recommended for Development)

Your OAuth consent screen is in **"Testing"** mode by default, which means:
- ✅ Only test users you added can sign in
- ✅ No Google verification needed
- ✅ Perfect for development
- ⚠️ Limited to 100 test users

**To test the app:**
1. Make sure you added your Google account as a test user (step 3.7)
2. Try signing in again on the app
3. You should see a consent screen saying "This app hasn't been verified" - click **"Continue"**
4. Grant the requested permissions

### Publishing (For Production - Later)

When ready to publish:
1. Go back to **"OAuth consent screen"**
2. Click **"Publish App"**
3. Submit for verification (Google will review your app)
4. Verification takes 3-7 days

**For now, keep it in Testing mode!**

---

## Troubleshooting

### "Access blocked" error persists
1. **Double-check package name**: Must be `com.loglife.app`
2. **Verify SHA-1**: Run this command to confirm:
   ```bash
   keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android | grep SHA1
   ```
3. **Wait 5-10 minutes**: Google Cloud changes can take time to propagate
4. **Clear app data**: Settings → Apps → LogLife → Clear Data
5. **Reinstall app**:
   ```bash
   adb uninstall com.loglife.app
   ./gradlew installDebug && adb shell am start -n com.loglife.app/.ui.MainActivity
   ```

### "This app hasn't been verified" warning
- **This is normal!** Click **"Continue"** (Advanced → Go to LogLife)
- This happens because the app is in Testing mode
- Only you (and your test users) can use it

### Wrong email shows up
- Make sure you're signed into the correct Google account on your device
- You can only use accounts listed as test users

---

## Security Notes

⚠️ **DO NOT commit OAuth credentials to git**
- The Android OAuth flow doesn't require a client secret
- Authentication is done via package name + SHA-1 fingerprint
- No sensitive files need to be added to the repo

✅ **Your app is secure because**:
- Package name is verified
- SHA-1 fingerprint prevents impersonation
- Only your signed APK can use these credentials

---

## Quick Reference

**Your App Info:**
- Package name: `com.loglife.app`
- Debug SHA-1: `C1:6C:1E:82:53:17:B0:C9:0A:93:C2:EB:5A:7E:79:C0:78:EC:DC:AF`

**Required APIs:**
- Google Drive API (for listing docs)
- Google Docs API (for writing to docs)

**Required Scopes:**
- `https://www.googleapis.com/auth/drive.readonly`
- `https://www.googleapis.com/auth/documents`

**Google Cloud Console:** https://console.cloud.google.com/

---

## Next Steps After Setup

Once you've completed the setup:

1. **Test the app:**
   ```bash
   ./gradlew installDebug && adb shell am start -n com.loglife.app/.ui.MainActivity
   ```

2. **Sign in** with the Google account you added as a test user

3. **Accept permissions** when prompted

4. **Pick a Google Doc** and start logging!

If you encounter issues, check the "Troubleshooting" section above.
