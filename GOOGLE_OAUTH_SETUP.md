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
6. **Scopes** (REQUIRED): Click **"Add or Remove Scopes"**
   - Search and add: `https://www.googleapis.com/auth/drive.readonly`
   - Search and add: `https://www.googleapis.com/auth/documents`
   - Click **"Update"** → **"Save and Continue"**
7. **Test users** (CRITICAL): Click **"Add Users"**
   - **Add your Google email address** (the one you'll use to test the app)
   - Click **"Add"** → **"Save and Continue"**
   - ⚠️ **Without this, you'll get "Access blocked" errors!**
8. Review and click **"Back to Dashboard"**
9. **Verify Publishing Status**: Should show **"Testing"** (not "In production")

### 4. Create Web OAuth Client ID (REQUIRED)
**Why needed:** Android apps need a Web OAuth client to obtain server auth codes for accessing Google APIs on behalf of users.

1. Go to **"APIs & Services"** → **"Credentials"**
2. Click **"Create Credentials"** → **"OAuth client ID"**
3. Select **"Web application"**
4. Fill in:
   - **Name**: `LogLife Web`
   - **Authorized redirect URIs**: Leave empty (not needed for Android flow)
5. Click **"Create"**
6. **Copy the Client ID** (format: `XXXXXX.apps.googleusercontent.com`)
   - Example: `401844581698-7cdsqunp7ebuiu8kbir5ko7jq63mraa0.apps.googleusercontent.com`
7. Click **"OK"**

**Note:** This Web client ID is already configured in the app code at `GoogleAuthManager.kt:29`. The app uses it to request server auth codes via `requestServerAuthCode()`.

### 5. Create Android OAuth Client (REQUIRED)
**Why needed:** Identifies your app via package name + SHA-1 fingerprint for the sign-in flow.

1. In **"Credentials"**, click **"Create Credentials"** → **"OAuth client ID"**
2. Select **"Android"** as application type
3. Fill in:
   - **Name**: `LogLife Android`
   - **Package name**: `com.loglife.app`
   - **SHA-1 certificate fingerprint**: Paste this:
     ```
     C1:6C:1E:82:53:17:B0:C9:0A:93:C2:EB:5A:7E:79:C0:78:EC:DC:AF
     ```
4. Click **"Create"**
5. **No download needed** - Android apps use the package name + SHA-1 for authentication

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
