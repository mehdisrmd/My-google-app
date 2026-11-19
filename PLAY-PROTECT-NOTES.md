# Play Protect hygiene audit — X Blocker v0.5 (batch item #25)

Audit performed for the v0.5 batch (group G12) over `AndroidManifest.xml`
and the full Kotlin source tree. Goal: remove every *avoidable* Play
Protect / "SafeGuard" trigger without breaking any feature. Play Protect
verdicts are computed server-side by Google from heuristics; this note
records what was checked, what changed, and what risk honestly remains.

## Permissions — kept (each is exercised by a named feature)

| Permission | Used by |
|---|---|
| `INTERNET` | Adult-blocklist downloads (#13, `AdultBlocklistUpdater`) — plain-text domain lists only, parsed as data, never executed. |
| `ACCESS_NETWORK_STATE` | Required by WorkManager's `NetworkType.CONNECTED` constraint for the weekly blocklist refresh (#13, `BlocklistUpdateWorker`). |
| `VIBRATE` | Lockdown notification channel vibration (`LockdownNotificationManager`). |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | The dashboard asks for the exemption so Android does not kill the accessibility service / enforcement (`ShieldPreferences.requestDisableBatteryOptimization`). |
| `POST_NOTIFICATIONS` | Lockdown + daily-streak notifications; requested at runtime from the dashboard permission card. |
| `RECEIVE_BOOT_COMPLETED` | `DailyStreakAlarmReceiver` re-arms the 9:00 AM streak alarm after reboot. |
| `USE_EXACT_ALARM` | The daily streak alarm is scheduled with `setExactAndAllowWhileIdle` (`LockdownNotificationManager`). |
| `PACKAGE_USAGE_STATS` | Per-app daily limits read foreground package + usage minutes via `UsageStatsManager` (`ShieldPreferences`). |

## Permissions — removed

| Permission | When | Why |
|---|---|---|
| `SYSTEM_ALERT_WINDOW` | v0.5 (G12) | **Never used.** No code adds an overlay window (no `WindowManager.addView`, no overlay window types anywhere). The block screen is a normal `Activity` (`LockdownActivity`). The permission only existed as a dashboard checklist ask ("Required for Block Screen") that sent users to grant a sensitive permission nothing consumed. Removed from the manifest, the checklist, the master-switch gate, and the `hasOverlayPermission()` helper. |
| `USE_BIOMETRIC` | v0.5 (G4, item #7) | Biometric unlock was removed entirely in favour of the mandatory PIN gate, so the permission went with it. |

Not declared, and must stay undeclared: `QUERY_ALL_PACKAGES` (the app
picker uses `queryIntentActivities` + a TTL cache instead),
`REQUEST_INSTALL_PACKAGES`, `REQUEST_DELETE_PACKAGES`,
`KILL_BACKGROUND_PROCESSES`, `READ_PHONE_STATE` (call detection in #17
deliberately uses `AudioManager.mode`, which needs no permission).

## Patterns checked — all absent

Verified by full-tree grep in this audit:

- Dynamic code loading: no `DexClassLoader` / `PathClassLoader` /
  `InMemoryDexClassLoader`, no `Class.forName`-based loading, no
  reflection into hidden APIs (`getDeclaredMethod`, `setAccessible`).
- No downloaded code: the only network fetches are the #13 text domain
  lists (data) — see the note in `AdultBlocklistUpdater`.
- No process control: no `Runtime.exec`, `ProcessBuilder`,
  `killBackgroundProcesses`, `killProcess`, `System.exit`.
- No self-concealment: no `setComponentEnabledSetting` (launcher icon is
  never hidden or disguised), no package renaming tricks.
- No WebView with a JavaScript bridge, no `DownloadManager` use.
- No obfuscation: release builds ship with `isMinifyEnabled = false`.

## Behaviours that look sensitive but are legitimate (kept, documented)

- **Accessibility service** — the app's stated core function. Its
  manifest description (`accessibility_service_description`) honestly
  names what it reads (on-screen text, website addresses) and why
  (blocking, app limits). `canRetrieveWindowContent` stays: without it
  there is no blocking at all.
- **`GLOBAL_ACTION_BACK` / `GLOBAL_ACTION_HOME`** in the accessibility
  service — standard accessibility navigation actions used to leave a
  blocked page. They do not kill or force-stop any app.
- **Device Admin receiver (#11)** — present only so the app *can* be
  provisioned as Device Owner by the user, outside the app. Its
  `device_admin.xml` requests an **empty** policy set (no password,
  wipe, or camera powers), and its description string says so.
- **`FLAG_SECURE`** on type-to-confirm fields (#5) — a standard
  anti-screenshot window flag, the opposite of a concealment pattern.

## Signing

Release builds are signed by CI with the project's single upload
keystore (`release` signing config in `app/build.gradle.kts`); debug
builds use a separate debug config and are never released. The signing
key must never be rotated between releases — a key change is itself a
trust signal and breaks updates.

## Restricted settings (Android 13+)

For sideloaded installs, Android blocks enabling the accessibility
service until the user opens App info → ⋮ → "Allow restricted
settings". **The app cannot bypass this** and does not try. Instead,
the dashboard permission card detects that the service is off and
offers a step-by-step guide (`RestrictedSettingsGuide.kt`, EN/FA).

## Honest residual risk

Play Protect classification is a server-side heuristic and cannot be
verified in CI or guaranteed in advance. An accessibility-driven
blocker installed outside Google Play is exactly the profile the
restricted-settings system was built for, so a warning on some device
or Android version remains possible even after this pass. What this
audit guarantees is narrower: the app declares no unused permissions,
loads no code, hides nothing, and describes itself honestly. Final
verification is installing the release APK on a real device with Play
Protect scanning enabled.
