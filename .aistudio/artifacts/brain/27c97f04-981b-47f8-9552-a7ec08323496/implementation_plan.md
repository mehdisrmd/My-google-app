# ShieldBlock - Screen Word & Adult Website Blocker

ShieldBlock is a digital well-being and content blocker inspired by BlockerX. It uses an Android Accessibility Service and Overlay permissions to inspect on-screen text and browser address bars for explicit/adult keywords and domains. When an illicit word is detected, it instantly kicks the user to home and triggers a strict, non-bypassable full-screen lockdown with a customizable countdown timer.

## User Review & Critical Decisions

> [!IMPORTANT]
> **Accessibility Service & Overlay Permissions**: Inspecting on-screen words across third-party browsers and apps requires enabling Android's `AccessibilityService` (`BIND_ACCESSIBILITY_SERVICE`) and optionally "Display over other apps" (`SYSTEM_ALERT_WINDOW`). The app provides a guided, frictionless setup card and an in-app **Test Sandbox** so you can safely verify word detection and lockdown timers without visiting real explicit sites.

- **Lock Reaction Confirmed**: Strict full-screen lockdown with customizable countdown timer (minutes/hours) and instant redirection away from explicit sites/apps.
- **Preloaded Keyword List Confirmed**: Adult and explicit content keywords (e.g., pornography terms, adult domain suffixes, explicit search phrases) preloaded, with full support for adding custom keywords and blacklisted domains.
- **Emergency Bypass Confirmed**: Strict lock mode enabled with zero bypass until the countdown timer reaches zero, featuring mindful urge-surfing exercises to help maintain self-discipline.

---

## 1. Overview & Core Concept

- **What It Does**:
  - Continuously monitors visible on-screen text nodes and browser URL bars using an Android `AccessibilityService`.
  - Matches visible content against a normalized list of adult keywords and blocked web domains.
  - When a trigger word is detected:
    1. Instantly executes `GLOBAL_ACTION_HOME` to close the offending browser/app.
    2. Launches the high-priority `LockdownActivity` with full-screen flags preventing back navigation or dismissal.
    3. Starts the strict countdown timer set by the user.
    4. Logs the block incident (timestamp, trigger keyword, and app package) to a local encrypted Room database.
- **Target Audience**: Individuals seeking freedom from adult content addictions, students, and professionals looking for unbreakable focus and self-discipline.
- **Key Value**: Immediate reflex protection. Unlike DNS blockers that can be bypassed or fail on certain apps, on-screen text matching catches browser tabs, search suggestions, social media posts, and app titles.

---

## 2. User Experience & Visual Design

### Key User Flows
1. **Onboarding & Permission Checklist**:
   - Status cards showing **Accessibility Service** and **Overlay Window** status with direct one-tap system settings shortcuts.
   - Clean explanation of why accessibility is needed (only scans for blocked keywords locally on-device; no data leaves the phone).
2. **Dashboard & Lock Controls**:
   - Active protection toggle switch.
   - Lock timer duration picker (presets: 1 min, 5 min, 15 min, 30 min, 1 hour, or custom minute slider).
   - "Test Protection" sandbox where users can type words into a test input to safely witness the detection and lockdown experience.
3. **Keyword & Website Rules Manager**:
   - Tabbed view: **Adult / Explicit Filter** (toggleable system presets) and **Custom Blacklist** (add/edit/delete specific terms or website URLs).
   - Sensitivity setting (strict matching vs. substring matching).
4. **Strict Lockdown Screen**:
   - Full-bleed dark obsidian theme with glowing crimson/amber shield badge.
   - Prominent real-time countdown timer (`MM:SS`).
   - Encouraging urge-surfing guidance ("Take deep breaths. This urge will peak and pass in 3 minutes").
   - Interactive 4-7-8 breathing circle animation to help calm impulses during the lockout.
   - Back button disabled via `BackHandler`; task pinned with `FLAG_ACTIVITY_NEW_TASK`.
5. **Activity & Incident Log**:
   - History list of blocked triggers with timestamps, masked trigger words, and streak counter showing days clean.

### Visual Identity & Theme
- **Theme**: Obsidian Guardian (Deep Midnight Slate `#0B0F19`, Deep Navy Card `#131B2E`, Electric Red Accent `#EF4444`, Neon Emerald Shield `#10B981` for active status).
- **Typography**: Clean, high-legibility sans-serif with bold tabular figures for the countdown timer.
- **Micro-Interactions**: Pulsing halo around active shield badge, smooth animated progress ring on the lock screen, haptic feedback on keyword trigger.

---

## 3. Key Product Decisions & Trade-Offs

- **Accessibility Service Detection**:
  - *Chosen Approach*: `AccessibilityService` listening to `TYPE_WINDOW_CONTENT_CHANGED` and `TYPE_WINDOW_STATE_CHANGED`, recursively checking `AccessibilityNodeInfo` text and content descriptions.
  - *Why*: Allows zero-root, universal detection across all browsers (Chrome, Firefox, Brave, Samsung Internet) and social media apps.
  - *Alternatives Considered*: VPN/DNS filtering alone misses on-screen search text, incognito titles, and app-based explicit material.
- **Strict Lockdown Enforcement**:
  - *Chosen Approach*: Combination of `performGlobalAction(GLOBAL_ACTION_HOME)` + launching `LockdownActivity` with `FLAG_ACTIVITY_REORDER_TO_FRONT | FLAG_ACTIVITY_NEW_TASK` and `WindowCompat` edge-to-edge full screen.
  - *Why*: Guarantees immediate eviction from the inappropriate content and keeps the lock screen visible throughout the configured duration.
- **Local Persistence via Room & StateFlow**:
  - *Chosen Approach*: Room Database for blocked keywords, custom words, and block incident logs; `SharedPreferences`/Room for lock state timestamps.
  - *Why*: Fast, 100% on-device, fully private, and persistent across device reboots.

---

## 4. Technical Architecture & Data Strategy

### Architecture & Component Diagram

```
┌──────────────────────────────────────────────────────────────┐
│                    Android System UI                         │
│  (Browsers, Search Apps, Social Media, Screen Text)          │
└──────────────────────────────┬───────────────────────────────┘
                               │ Accessibility Events (NodeInfo)
                               ▼
┌──────────────────────────────────────────────────────────────┐
│            ShieldBlockAccessibilityService                   │
│  - Traverses AccessibilityNodeInfo tree                      │
│  - Normalized keyword & domain matcher                       │
│  - Triggers GLOBAL_ACTION_HOME                               │
│  - Dispatches Intent -> LockdownActivity                     │
└──────────────┬───────────────────────────────┬───────────────┘
               │ Matches Trigger               │ Reads Rules
               ▼                               ▼
┌──────────────────────────────┐ ┌─────────────────────────────┐
│       LockdownActivity       │ │      AppDatabase (Room)     │
│ - Strict non-dismissable UI  │ │ - BlockedKeywordEntity      │
│ - Live Countdown Timer       │ │ - BlockIncidentEntity       │
│ - 4-7-8 Breathing Guide      │ │ - UserPreferencesStore      │
│ - BackHandler locked         │ └──────────────┬──────────────┘
└──────────────────────────────┘                │ Observes
                                                ▼
┌──────────────────────────────────────────────────────────────┐
│                   Main Dashboard Activity                    │
│  - Permission Status & Setup Cards                           │
│  - Custom Timer Duration Selector (1m - 60m)                 │
│  - Preloaded Adult Keywords & Custom Blacklist Manager       │
│  - Safe Test Sandbox / Simulator                             │
│  - Incident History & Recovery Streak Counter                │
└──────────────────────────────────────────────────────────────┘
```

### Data Models & Entities
1. **`BlockedKeywordEntity`**: `id`, `keyword`, `category` (ADULT, CUSTOM, DOMAIN), `isEnabled`, `isRegex`.
2. **`BlockIncidentEntity`**: `id`, `timestamp`, `keywordMatched`, `sourcePackageName`, `lockDurationMinutes`.
3. **`LockState`**: `isLocked`, `lockEndTime`, `totalDurationSeconds`, `reason`.

### Interactive Component Mapping
- **Permission Monitor**: Periodic broadcast/state check to verify `isAccessibilityServiceEnabled()` and highlight setup steps.
- **Test Sandbox Composable**: Live text field allowing users to type test keywords (e.g. `test-porn`, `xxx`) to preview the exact lock sequence safely.
- **Lock Screen**: Handles countdown ticking via `LaunchedEffect(lockEndTime)`, dynamically updates remaining minutes/seconds, and only allows dismissing when `remainingMillis <= 0`.
