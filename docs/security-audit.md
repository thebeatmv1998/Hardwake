# Security audit

**App:** Hardwake (`com.wecker.app`)  
**Version:** 1.2.0 (versionCode 3)  
**Date:** 2026-10-25  
**Scope:** Android manifest, exported components, intents and PendingIntents, storage, permissions, build configuration, dependencies, review of CVE-2026-91403.  
**Method:** manual source review of the app and the merged release manifest. No dynamic testing on a device or emulator.

## Threat model

Hardwake keeps all state in app-private storage. The relevant attackers are:

1. Another app on the same device that tries to trigger components or read data.
2. Someone with physical access to an unlocked device.
3. Network attackers. The app code makes no network requests. The merged manifest does contain `INTERNET`, inherited from ML Kit's telemetry transport, so third-party library traffic is possible but not controlled by app code (see S-5).

Alarm times, labels and learned barcode values are personal but not secrets in the cryptographic sense. The goal is to stop other apps from interfering with alarms or reading that data.

## Findings

| ID | Severity | Finding | Status |
| --- | --- | --- | --- |
| S-1 | Medium | `BootReceiver` had `android:exported="true"`. Any installed app could send a fake `BOOT_COMPLETED` and force a full alarm reschedule. | **Fixed.** Set to `false`. Broadcast receivers still receive system broadcasts when not exported. |
| S-2 | Medium | `android:allowBackup` was not set, so it defaulted to `true`. Alarm labels, times and barcode values could end up in cloud backups or device-to-device transfers. | **Fixed.** `android:allowBackup="false"`. |
| S-3 | Low | PendingIntents must not be mutable, or another app can fill in the intent. | **Verified.** Every PendingIntent uses `FLAG_IMMUTABLE` and an explicit target component. |
| S-4 | Low | The dismiss broadcast is implicit, so other apps could listen for it. | **Verified.** The intent is restricted with `setPackage(packageName)`. |
| S-5 | Low | Network access. The app code uses no networking APIs, but the merged release manifest contains `INTERNET` and `ACCESS_NETWORK_STATE`. The manifest merger attributes both to `com.google.android.datatransport:transport-backend-cct`, a transitive dependency of ML Kit telemetry. | **Accepted, documented.** No app code path uses the network. On-device barcode scanning does not require it. Removing both permissions with `tools:node="remove"` would restore a zero-network build; verify barcode scanning afterwards. |
| S-6 | Low | Alarm labels and barcode values are stored in an unencrypted Room database. | **Accepted.** The database is in app-private storage, excluded from backups, and cannot leave the device. Root access defeats any local encryption. |
| S-7 | Info | The ringing notification uses `VISIBILITY_PUBLIC`, so the alarm label can show on the lock screen. | **Accepted by design.** A ringing alarm should be visible without unlocking. The wake-check notification carries no personal data. |
| S-8 | Info | `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` is a restricted permission for Google Play distribution. | **Accepted** for private/sideloaded distribution. Removing it requires a different onboarding flow. |
| S-9 | Info | Math problems use `kotlin.random.Random`, which is not a cryptographic generator. | **Not a security boundary.** Answers are computed and checked on the same device. |
| S-10 | Low | Dependency versions date from 2024: Compose BOM 2024.10.01, CameraX 1.3.4, Room 2.6.1, ML Kit 17.3.0. | **Open.** Update dependencies and repeat this review before a public release. |
| S-11 | Info | APK signature schemes. v1 (JAR signing) is unnecessary for `minSdk 26`. | **Verified.** The 1.2.0 APK is signed with v2 and v3; v1 is disabled, and `apksigner verify` passes. |
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

## Reviewed CVEs

### CVE-2026-91403 (Oracle Java SE / GraalVM, JSSE)

**Not affected.** The advisory covers Oracle Java SE 8u511, 11.0.33, 17.0.21, 21.0.13, 25.0.5, 27.0.1 and GraalVM for JDK 17.0.21 / 21.0.13 plus GraalVM Enterprise 21.3.20. The flaw is in Oracle's JSSE implementation.

- The app ships no Java runtime. An APK contains DEX bytecode that runs on Android Runtime (ART), not on Oracle Java SE or GraalVM.
- Android's TLS stack is Conscrypt and the platform providers, not Oracle JSSE. The app imports no `javax.net.ssl` or `java.net` classes, and an earlier `grep` over `app/src` finds no networking APIs.
- The build toolchain uses Eclipse Temurin 17.0.19 (OpenJDK) as a build-time JDK only. It is not packaged into the APK and does not accept inbound TLS connections.
- The `INTERNET` permission from ML Kit (S-5) does not change this: any library traffic uses Android's TLS stack.

Recommended build hygiene: move the Gradle JDK to a current Temurin release. That protects the build machine, not the shipped app.

## Recommendations

1. Update the AndroidX, CameraX, Room and ML Kit dependencies, then repeat this review.
2. If the app ever ships on Google Play, remove `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` and rely on the exact-alarm settings screen that the app already links to.
3. Keep `allowBackup=false` unless alarm data should sync to another device, in which case add explicit backup rules and encrypt the database.

## Out of scope

- Vulnerabilities in Android itself or in the device vendor image.
- Attacks that require root or physical access to an unlocked device.
- Google Play policy compliance beyond the permission note above.
