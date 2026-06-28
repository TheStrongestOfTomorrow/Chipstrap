<div align="center">

# Chipstrap

[![GitHub Downloads](https://img.shields.io/github/downloads/TheStrongestOfTomorrow/Chipstrap/total)](https://github.com/TheStrongestOfTomorrow/Chipstrap/releases)
[![GitHub License](https://img.shields.io/github/license/TheStrongestOfTomorrow/Chipstrap)](LICENSE.txt)
[![GitHub Tag](https://img.shields.io/github/v/tag/TheStrongestOfTomorrow/Chipstrap)](https://github.com/TheStrongestOfTomorrow/Chipstrap/releases)
[![GitHub Workflow Status](https://img.shields.io/github/actions/workflow/status/TheStrongestOfTomorrow/Chipstrap/release.yml)](https://github.com/TheStrongestOfTomorrow/Chipstrap/actions/workflows/release.yml)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.0-7F52FF)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-BOM%202024.12-4285F4)](https://developer.android.com/jetpack/compose)
[![Min Android](https://img.shields.io/badge/min%20Android-8.0%20(API%2026)-green)](https://github.com/TheStrongestOfTomorrow/Chipstrap)
[![Target Android](https://img.shields.io/badge/target%20Android-14%20(API%2034)-orange)](https://github.com/TheStrongestOfTomorrow/Chipstrap)

</div>

> [!CAUTION]
> The only official place to download Chipstrap is this GitHub repository. Any other websites offering downloads or claiming to be us are not controlled by us.

# Chipstrap

Chipstrap is a Roblox & Roblox VN launcher for Android, written in **100% Kotlin + Jetpack Compose** for native speed. Inspired by [Bloxstrap](https://github.com/bloxstraplabs/bloxstrap) and forked from the abandoned [Chevstrap](https://github.com/FrosSky/Chevstrap) project — Chipstrap restores FFlag management that Roblox disabled in client 2.650+ via a multi-strategy injector.

## Features

### FFlag management
- Adjust specific FFlags for testing or performance
- 6 pre-optimized presets: Ultra FPS, Balanced, Battery Saver, High Quality, Low-End Device, Competitive (uncapped FPS)
- Import / Export JSON (BloxStrap-compatible)
- Backups / Restore
- Per-flag add / edit / delete

### Multi-strategy injection (workaround for Roblox disabling FFlags)
Chipstrap works around Roblox 2.650+ no longer honoring `ClientAppSettings.json` outside its private data dir via four injection strategies, picked automatically at runtime:

| Strategy | Requires | Reliable |
|---|---|---|
| **Shizuku** | Shizuku running + binder granted | ✅ |
| **Root** | Rooted device | ✅ |
| **Virtual space** | Parallel Space / DualSpace | ⚠️ |
| **Local profile** | Nothing | ❌ (export only) |

### Optimizations
- Clear Roblox cache before launch
- Kill background apps
- Force CPU performance governor (root)
- Anti-Doze wakelock during gameplay
- GPU tuning (disable animation scale)
- Bluetooth audio buffer boost
- Aggressive memory trim
- Low-latency private DNS (1.1.1.1)

### BloxStrap-inspired
- See where your server is currently located (via [ipinfo.io](https://ipinfo.io/) API)
- Real-time server info (JobID / place / universe / ping)
- Activity tracker (session duration + recent experiences)
- Able to rejoin your last server after leaving it

### Multi-version Roblox support
- Global client (`com.roblox.client`)
- Vietnam client (`com.roblox.client.vnggames`)
- Custom package

## Installation

1. Download the [latest release of Chipstrap](https://github.com/TheStrongestOfTomorrow/Chipstrap/releases).
2. Install the APK on your Android device (`adb install -r Chipstrap-release-*.apk`).
3. Once installed, open Chipstrap and grant the notification permission.
4. Go to **Integrations → Application strategy** and pick your injection strategy (Shizuku recommended — no root needed).
5. Go to **FFlags**, apply a preset (or add your own).
6. Go back to **Home → Apply & Launch**.

## Requirements

- Android 8.0 (API 26) or newer
- Roblox installed (Global or VNG)
- One of the following for FFlag injection:
  - [Shizuku](https://shizuku.rikka.app/) running on the device (recommended, no root), or
  - A rooted device, or
  - A virtual-space app (Parallel Space / DualSpace) with Roblox cloned inside

If none of the above are available, Chipstrap will still keep your FFlag profile locally for export.

## Build

Requirements: JDK 17, Android SDK 35, Kotlin 2.1.

```bash
git clone https://github.com/TheStrongestOfTomorrow/Chipstrap.git
cd Chipstrap
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

For a release build (self-signed with the debug key if no `app/release.keystore` exists):

```bash
./gradlew assembleRelease
# APK: app/build/outputs/apk/release/app-release.apk
```

Every push to `main` triggers the [Release workflow](.github/workflows/release.yml) which auto-builds both debug and release APKs and creates a GitHub release with them attached.

## Setup Shizuku (recommended, no root)

1. Install [Shizuku](https://shizuku.rikka.app/) from Google Play.
2. Start Shizuku via wireless debugging (Android 11+) or ADB.
3. In Chipstrap → Integrations → Application strategy → **Shizuku**.
4. Done — Chipstrap will use Shizuku to write `ClientAppSettings.json` into Roblox's private data directory on every launch.

## F.A.Q

#### Q: Is this malware?

A: No, it's not malware. The source code is viewable to all, so anyone can verify that it doesn't do anything harmful. If anything was detected, that's a false positive. Just make sure you're downloading it from the official GitHub repository.

#### Q: Can I get banned for using Chipstrap?

A: It shouldn't. Chipstrap doesn't interact with the Roblox client process directly — it only writes a `ClientAppSettings.json` file into Roblox's data directory before launch (same mechanism Roblox's own engine supports for its engineers). However, modifying the Roblox client may violate Roblox's Terms of Service, so use at your own risk.

#### Q: Why does Chipstrap need Shizuku or root?

A: Roblox 2.650+ stopped honoring `ClientAppSettings.json` placed outside its private data directory. On modern Android (10+), apps can't write to other apps' private data dirs without elevated privileges. Shizuku or root is required to bypass that restriction.

#### Q: Will this survive Roblox updates?

A: Yes. Chipstrap resolves the live Roblox data directory on every launch (via `pm path`), so it always finds the right target even after Roblox updates.

#### Q: The original Chevstrap was abandoned — what changed?

A: Chevstrap's README literally read `FFLAGS DISABLED BY ROBLOX ITSELF. DISCONTINUED`. Roblox disabled external FFlag reads by tightening Android's scoped storage on the client data dir. Chipstrap works around it with a multi-strategy injector (Shizuku / Root / Virtual space / Local) and is a complete Kotlin + Jetpack Compose rewrite for native performance.

#### Q: Can you add Discord Rich Presence?

A: Not currently — Discord's Android SDK has known stability issues. May revisit in the future.

#### Q: Why Kotlin + Jetpack Compose?

A: Because the user asked for it. Native Kotlin compiles to the same JVM bytecode as Java but with less boilerplate, and Jetpack Compose is Google's modern declarative UI toolkit — both compile directly to native Android (no WebView, no React Native bridge), giving very fast startup and low memory usage.

## Project layout

```
app/src/main/kotlin/com/chipstrap/rbx/
├── ChipstrapApp.kt             # Application entrypoint (hardened init)
├── MainActivity.kt             # Compose host + navigation
├── core/Logger.kt              # File logger
├── data/
│   ├── AppPaths.kt             # All on-device paths
│   └── SettingsStore.kt        # DataStore-backed preferences
├── fflags/
│   ├── presets/FFlagPresets.kt # Pre-tuned FFlag bundles
│   ├── repository/             # JSON-backed flag store
│   └── strategies/             # Shizuku / Root / Virtual / Local injectors
├── optimization/               # CPU/cache/wakelock/DNS optimizations
├── roblox/                     # Package helpers + launcher pipeline
├── server/                     # Server info provider (BloxStrap-style)
├── activity/                   # Activity tracker
├── service/                    # Launcher foreground service
└── ui/                         # Compose UI (Material 3)
```

## Disclaimer

Chipstrap is **not affiliated with Roblox Corporation**. Modifying the Roblox client may violate Roblox's Terms of Service. Use at your own risk.

## Credits

- [Chevstrap](https://github.com/FrosSky/Chevstrap) — original Android port this project was forked from.
- [BloxStrap](https://github.com/bloxstraplabs/bloxstrap) — Windows Roblox bootstrapper that inspired the server-info and activity-tracking features.
- [Roblox Client Tracker](https://github.com/MaximumADHD/Roblox-Client-Tracker) — flag reference.

## License

MIT. See [LICENSE.txt](LICENSE.txt).
