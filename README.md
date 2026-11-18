# TimeGuard V2 — Android local test app

This is a standalone Android/Kotlin prototype, **not** a production parental-control app or live AI service.

## Features
- Scroll-event recording via Android AccessibilityService, with no window-content retrieval.
- Local per-app event counts; heuristic session count and estimated active minutes (events within 60 seconds, same app).
- Local rule-based Screen Coach and configurable daily estimated-minutes goal.
- Local data deletion and no network permission, account or cloud upload.

## Important limitations
- Event counts are **not swipes**. Session duration is an approximation and can severely undercount idle periods or miss unsupported apps.
- No actual screen blocking, parent PIN, remote management, AI API, background notifications or exact screen time.
- Accessibility services expose sensitive information; install only trusted builds. Android may require explicitly allowing restricted settings for a sideloaded APK.
- Event storage uses SharedPreferences JSON and is capped at roughly 20,000 events / 7 days; suitable only for testing, not scalable production use.
- Debug APKs built on different GitHub runners may have different signing keys. Uninstalling a previous debug build may be required, which deletes local app data.

## GitHub web upload on Android
1. Extract this ZIP in Samsung My Files.
2. Open your GitHub repository in browser Desktop site mode.
3. Recommended: create a **new empty repository** for V2 to avoid stale V1 files. In the repository select **Add file → Upload files**.
4. GitHub web upload may not preserve folders when selecting them from Android's picker. Use a computer to upload the extracted directory recursively, **or** use GitHub's web editor to create each file using the exact paths shown in this ZIP. Do not upload the ZIP itself expecting GitHub to extract it.
5. Ensure `.github/workflows/android-apk.yml`, `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`, `app/src/main/java/com/timeguard/app/{MainActivity,ScrollService,ScrollStore}.kt`, and `app/src/main/res/{xml/accessibility_service_config.xml,values/strings.xml}` are in their proper locations.
6. Actions → Build TimeGuard V2 APK → Run workflow (or push to main). Download artifact ZIP from successful run; extract and install `app-debug.apk`.

## Local build
Use Android Studio with JDK 17, Android SDK 35 and Gradle 8.9. Run `gradle :app:assembleDebug`.

## Test
Enable the TimeGuard scrolling test accessibility service; scroll in TikTok, return to dashboard and refresh. Check event counts and coaching. Disable service after testing if desired.
