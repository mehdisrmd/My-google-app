# X Blocker — Smart Section Blocker, Content Shield & App Limiter for Android

**X Blocker** (`v2.1.0`) is a 100% on-device Android self-control, focus, and content protection app built with **Kotlin**, **Jetpack Compose (Material 3)**, **Room**, and **Android Accessibility / UsageStats APIs**.

- **Developer Telegram Contact:** [@mehdisrmd](https://t.me/mehdisrmd)

---

## Key Features

### 1. In-App Section Blocker (Stop Section Only — Not the Whole App!)
- **Instagram Reels Blocker**: Stops **only** the Reels section (`GLOBAL_ACTION_BACK`) when you enter the Reels tab in Instagram—allowing you to use Instagram Direct Messages, Stories, and Home Feed normally.
- **Telegram Search Box Blocker**: Stops **only** the Search Box (`GLOBAL_ACTION_BACK`) when you tap/focus the search input box in Telegram—allowing you to read and send messages in Telegram chats normally.
- Prominent quick toggles right in the **middle of the Dashboard** (`In-App Section Guard`).

### 2. Bilingual Persian (فارسی RTL) & English + Dark & White Theme
- Instant **tiny toggle buttons** in the top bar for:
  - **Persian (`FA | فارسی`) with full Right-to-Left (`RTL`) layout** vs. **English (`EN`) Left-to-Right (`LTR`)**.
  - **Dark Theme** vs. **White (Light) Theme**.

### 3. Info Page & Automatic Background GitHub Update Checker
- Dedicated **Info** tab showing app version (`v2.1.0`), clickable developer Telegram contact (**`@mehdisrmd`**), and public GitHub repository configuration.
- Automatically checks GitHub Releases (`https://api.github.com/repos/{owner}/{repo}/releases/latest`) in the background every time you enter the app, plus a manual **Check for Update** button.

### 4. Daily App Usage Limits with 2-Hour Lock Cooldown & Working Refresh Button
- Set daily usage timers (15m to 8h) on any installed app or popular social/media preset with a strict **2-hour modification cooldown**.
- Includes a dedicated **Refresh / Reset Timer** button on every app limit card (and in the header) that immediately resets today's usage baseline and refreshes the UI.
- Starts completely clean with **zero predefined example apps** in both App Limits and Weekly Activity Status.

### 5. Simplified Anti-Agent & Screenshot Blocker (`FLAG_SECURE`)
- Clean single **ON / OFF** switch with a clear explanation: turn ON so AI agents, screen recorders, and screenshots only see a black screen (`FLAG_SECURE`), or leave OFF (default) for normal screen viewing and screenshots.

---

## GitHub Actions Automatic APK Release (`.github/workflows/android-ci.yml`)
Every push to `main`/`master` automatically builds **`X-Blocker.apk`** and publishes it directly to **GitHub Releases**.
