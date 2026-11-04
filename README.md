# ShieldBlock — On-Device Focus, App Limiter & Content Guard for Android

**ShieldBlock** is a 100% on-device Android self-control, focus, and content protection app built with **Kotlin**, **Jetpack Compose (Material 3)**, **Room**, and **Android Accessibility / UsageStats APIs**.

---

## Key Features

### 1. Real-Time English & Persian Keyword & Domain Blocker
- Scans on-screen text in real time using `ShieldAccessibilityService` without ever transmitting screen data off-device.
- Full support for **English** and **Persian (فارسی)** keywords, phrases, and adult domains, including Arabic/Persian character normalization (`ي`/`ی`, `ك`/`ک`, diacritic stripping, and half-space handling) and leetspeak detection.
- **Batch Multi-Word Import**: Add multiple words, phrases, or domains at once separated by commas or newlines.

### 2. Daily App Usage Limits with Strict 2-Hour Cooldown
- Set daily usage timers (15m to 8h) on any installed device app or popular social/media preset (**Instagram**, **Telegram**, **YouTube**, **TikTok**, **Chrome**, **X**, **WhatsApp**, **Discord**, **Reddit**, etc.).
- **Multi-Client Variant Resolution (`AppPackageResolver`)**: Automatically tracks and blocks across official and alternative package variants (e.g., Telegram, Telegram X, Plus Messenger, Nekogram, Instagram Lite, YouTube Music, Chrome Beta/Dev).
- **Strict 2-Hour Cooldown Lock**: Once an app limit is saved or modified, it is locked for **2 hours**—preventing impulsive disabling or timer increases during moments of urge.

### 3. In-App Section Blocker
- Block specific high-distraction tabs or sections inside apps without blocking the entire app:
  - **Instagram Reels**
  - **Telegram Global Search (`search` / `جستجو`)**
  - **YouTube Shorts**
  - Custom user-defined section rules.

### 4. Smart Focus (Deep Work Auto-Lock Schedule)
- Schedule recurring **Deep Work** time slots by start time, duration, and active days of the week.
- Automatically triggers a temporary phone lock session when a scheduled Deep Work window begins, or launch an instant **15m / 25m / 45m / 60m Deep Work Sprint** with one tap.

### 5. Emergency Panic Mode (SOS) & Always-On Notification Bar Action
- Persistent **Always-On Panic SOS Notification** in the Android status bar with instant actions (`PANIC SOS` and `LOCK NOW`).
- Guided **4-7-8 Breathing Cadence**, personal commitment pledge, urge counter, and 1-tap voluntary emergency lockdown.

### 6. NoBeep-Inspired Streak Counter & Home Screen Widget
- Live clean-streak timer (days, hours, minutes, seconds), milestone badges (*Novice*, *Spark*, *Bronze Shield*, *Silver Guardian*, *Gold Warrior*, *Diamond Master*), slip logging, and a 7-Day **Weekly Activity & Discipline Score** dashboard.
- Includes a resizable **Home Screen AppWidget** (`StreakWidgetProvider`) displaying your live streak and current milestone badge.

### 7. Biometric Lodge & Anti-Agent Privacy Blackout Shield (`FLAG_SECURE`)
- **Biometric Lodge**: Protect app settings and rule modifications with Android Biometric authentication (fingerprint/face) + fallback PIN.
- **Anti-Agent Blackout Shield (`FLAG_SECURE`)**: Optional hardware-level screen capture and AI/screen-recording blackout protection, with a **60-second Personal Screenshot Unlock Window** when you want to capture your own progress.

---

## Tech Stack & Architecture
- **UI**: Jetpack Compose, Material Design 3, Custom Obsidian & Emerald/Crimson/Indigo Telemetry Theme
- **Persistence**: Room Database (`ShieldDatabase`, `ShieldDao`) + `SharedPreferences` reactive `StateFlow` layer (`ShieldPreferences`)
- **System Integration**: `AccessibilityService`, `UsageStatsManager`, `BiometricPrompt`, `AppWidgetProvider`, Foreground/Persistent Notifications
- **Testing**: Local JVM & Robolectric test suite (`ExampleRobolectricTest`)

---

## Building & Running Tests

```bash
# Assemble Debug APK
gradle assembleDebug

# Run Unit & Robolectric Tests
gradle :app:testDebugUnitTest
```

---

## GitHub Actions CI/CD (`.github/workflows/android-ci.yml`)
Every push or pull request to `main`/`master` (or manual trigger via **Actions → Run workflow**) automatically:
1. Sets up JDK 21, Android SDK 36, and Gradle (`gradle-wrapper.properties`).
2. Restores `debug.keystore` from `debug.keystore.base64` and initializes `.env`.
3. Runs the full **Robolectric & JUnit test suite** (`gradle :app:testDebugUnitTest`) and uploads the HTML test report.
4. Builds the signed installable **Debug APK** (`gradle :app:assembleDebug`) and uploads `ShieldBlock-Debug-APK` as a downloadable workflow artifact (and attaches it to GitHub Releases on `v*` tags).

