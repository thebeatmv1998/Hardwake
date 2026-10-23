# Security audit

**App:** Hardwake (`com.wecker.app`)  
**Version:** 1.1.0 (versionCode 2)  
**Date:** 2026-10-23  
**Scope:** Android manifest, exported components, intents and PendingIntents, storage, permissions, build configuration, dependencies.  
**Method:** manual source review. No dynamic testing on a device or emulator.

## Threat model

Hardwake runs offline and keeps all state in app-private storage. The relevant attackers are:

1. Another app on the same device that tries to trigger components or read data.
2. Someone with physical access to an unlocked device.
3. Network attackers. The app has no network permission, so this surface is empty.

Alarm times, labels and learned barcode values are personal but not secrets in the cryptographic sense. The goal is to stop other apps from interfering with alarms or reading that data.

## Findings

| ID | Severity | Finding | Status |
| --- | --- | --- | --- |
| S-1 | Medium | `BootReceiver` had `android:exported="true"`. Any installed app could send a fake `BOOT_COMPLETED` and force a full alarm reschedule. | **Fixed.** Set to `false`. Broadcast receivers still receive system broadcasts when not exported. |
| S-2 | Medium | `android:allowBackup` was not set, so it defaulted to `true`. Alarm labels, times and barcode values could end up in cloud backups or device-to-device transfers. | **Fixed.** `android:allowBackup="false"`. |
| S-3 | Low | PendingIntents must not be mutable, or another app can fill in the intent. | **Verified.** Every PendingIntent uses `FLAG_IMMUTABLE` and an explicit target component. |
| S-4 | Low | The dismiss broadcast is implicit, so other apps could listen for it. | **Verified.** The intent is restricted with `setPackage(packageName)`. |
| S-5 | Low | Data exfiltration over the network. | **Verified.** The manifest declares no `INTERNET` permission and no other network permission. |
| S-6 | Low | Alarm labels and barcode values are stored in an unencrypted Room database. | **Accepted.** The database is in app-private storage, excluded from backups, and cannot leave the device. Root access defeats any local encryption. |
| S-7 | Info | The ringing notification uses `VISIBILITY_PUBLIC`, so the alarm label can show on the lock screen. | **Accepted by design.** A ringing alarm should be visible without unlocking. The wake-check notification carries no personal data. |
| S-8 | Info | `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` is a restricted permission for Google Play distribution. | **Accepted** for private/sideloaded distribution. Removing it requires a different onboarding flow. |
| S-9 | Info | Math problems use `kotlin.random.Random`, which is not a cryptographic generator. | **Not a security boundary.** Answers are computed and checked on the same device. |
| S-10 | Low | Dependency versions date from 2024: Compose BOM 2024.10.01, CameraX 1.3.4, Room 2.6.1, ML Kit 17.3.0. | **Open.** Update dependencies and repeat this review before a public release. |
| S-11 | Info | The release APK carries v1, v2 and v3 signatures. v1 is unnecessary for `minSdk 26`. | **Open.** Future releases can be signed with v2/v3 only. |
| S-12 | Info | Signing material could leak through the repository. | **Verified.** `keystore/`, `*.jks` and `keystore.properties` are gitignored, and the keystore is not tracked. |
| S-13 | Low | Debug builds are debuggable and allow backup by default. | **Verified.** Release builds set `debuggable=false` and now disable backups. |
| S-14 | Info | The ring screen could appear in the recents list and leak alarm content. | **Verified.** `RingActivity` is `exported=false` and `excludeFromRecents=true`. |

## Component inventory

| Component | Exported | Notes |
| --- | --- | --- |
| `MainActivity` | yes | Launcher activity. Reads no intent extras and opens no URLs from input. |
| `RingActivity` | no | Full-screen alarm screen, excluded from recents. |
| `AlarmService` | no | Media playback foreground service. |
| `AlarmReceiver` | no | Exact alarm trigger. |
| `WakeCheckReceiver` | no | Wake-check trigger and final dismissal. |
| `BootReceiver` | no after S-1 | Reschedules alarms after boot. |

The app declares no content providers, no file providers and no WebView.

## Recommendations

1. Update the AndroidX, CameraX, Room and ML Kit dependencies, then repeat this review.
2. If the app ever ships on Google Play, remove `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` and rely on the exact-alarm settings screen that the app already links to.
3. Sign future APKs with v2/v3 only.
4. Keep `allowBackup=false` unless alarm data should sync to another device, in which case add explicit backup rules and encrypt the database.

## Out of scope

- Vulnerabilities in Android itself or in the device vendor image.
- Attacks that require root or physical access to an unlocked device.
- Google Play policy compliance beyond the permission note above.
