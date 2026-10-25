# Security audit

**App:** Hardwake (`com.wecker.app`)  
**Version:** 1.2.2 (versionCode 5)  
**Date:** 2026-10-25  
**Method:** manual source review, merged release manifest, Android Lint (`lintRelease`), OSV.dev dependency scan, APK inspection with `aapt`/`apksigner`, DEX string scan, git history secret scan. No dynamic testing on a device or emulator.

## Summary

| Severity | Open | Fixed in this pass | Accepted |
| --- | --- | --- | --- |
| Critical | 0 | 0 | 0 |
| High | 0 | 0 | 0 |
| Medium | 0 | 1 | 0 |
| Low | 0 | 1 | 2 |
| Info | 0 | 1 | 4 |

No open high or medium findings. The remaining open item is dependency currency (S-10).

## Scope

- All Kotlin sources under `app/src/main`.
- The merged release manifest, not just the source manifest.
- The signed release APK: permissions, components, signature, DEX strings, native libraries.
- Room database and DataStore contents.
- Build configuration, R8, signing, backup rules.
- Resolved Maven dependencies (117 artifacts) against OSV.dev.
- Repository and git history for secrets and signing material.

## Threat model

Hardwake stores alarms, labels and learned barcode values. Relevant attackers:

1. Another app on the same device that tries to trigger components or read data.
2. Someone with physical access to an unlocked device.
3. Network attackers. The app has no `INTERNET` permission, so it cannot open sockets.

Alarm data is personal but not cryptographic-grade secret. The security goal is to keep other apps from interfering with alarms or reading that data.

## Findings

| ID | Severity | Finding | Status |
| --- | --- | --- | --- |
| S-1 | Medium | `BootReceiver` was exported; any app could send a fake `BOOT_COMPLETED` and force a reschedule. | **Fixed** in 1.1.0. `exported=false`. |
| S-2 | Medium | `allowBackup` defaulted to `true`; alarm data could reach cloud backups. | **Fixed** in 1.1.0. `allowBackup=false`. |
| S-3 | Low | Mutable PendingIntents would let another app fill in the intent. | **Verified.** Every PendingIntent uses `FLAG_IMMUTABLE` and an explicit component. |
| S-4 | Low | The dismiss broadcast is implicit. | **Verified.** Restricted with `setPackage(packageName)`. |
| S-5 | Low | ML Kit's telemetry transport injected `INTERNET` into the merged manifest. | **Fixed** in 1.2.1. `tools:node="remove"`. |
| S-6 | Low | Alarm labels and barcode values are stored in an unencrypted Room database. | **Accepted.** App-private storage, no backup, no network. Root defeats local encryption. |
| S-7 | Info | The ringing notification is `VISIBILITY_PUBLIC`, so the label can appear on the lock screen. | **Accepted.** A ringing alarm should be visible without unlocking. |
| S-8 | Info | `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` is restricted for Google Play. | **Accepted** for private distribution. |
| S-9 | Info | Math problems use `kotlin.random.Random`. | **Not a security boundary.** Answers never leave the device. |
| S-10 | Low | 13 dependencies are behind current releases (Compose BOM 2024.10.01, CameraX 1.3.4, Room 2.6.1, DataStore 1.1.1, Navigation 2.8.3, Activity 1.9.3, Lifecycle 2.8.6, desugar 2.1.2). | **Open.** No known vulnerabilities in the resolved versions (OSV). Update in stages. |
| S-11 | Info | v1 JAR signing is unnecessary for `minSdk 26`. | **Verified.** APK uses v2 + v3 only; `apksigner verify` passes. |
| S-12 | Info | Signing material could leak through the repository. | **Verified.** Keystore, `.jks`, `local.properties` are untracked and were never committed. |
| S-13 | Low | Debug builds are debuggable. | **Verified.** Release manifest has no `debuggable`, so R8 builds are not debuggable. |
| S-14 | Info | The ring screen could leak alarm content in recents. | **Verified.** `exported=false`, `excludeFromRecents=true`. |
| S-15 | Info | Build JDK was Temurin 17.0.19. | **Updated** in 1.2.1 to Temurin 17.0.20.1. Build-only, not shipped. |
| S-16 | Info | `androidx.profileinstaller.ProfileInstallReceiver` is exported. | **Accepted.** Guarded by `android.permission.DUMP` (signature/privileged); only shell or system can trigger it. |
| S-17 | Medium | `allowBackup=false` covers cloud backups, but Android 12+ device-to-device transfer needs `dataExtractionRules`. | **Fixed** in 1.2.2. `fullBackupContent=false` plus a `dataExtractionRules` file that excludes every domain for cloud and device transfer. |
| S-18 | Low | Full lint failed on `UnsafeOptInUsageError` in `BarcodeScanner`. | **Fixed** in 1.2.2 with `androidx.annotation.OptIn(ExperimentalGetImage::class)`. Lint now passes with 0 errors. |
| S-19 | Info | ML Kit ships a telemetry transport (`transport-backend-cct`) and keeps `ACCESS_NETWORK_STATE`. | **Accepted.** Read-only connectivity state, no transmission without `INTERNET`. |

## Permissions

| Permission | Source | Protection | Purpose |
| --- | --- | --- | --- |
| `SCHEDULE_EXACT_ALARM` | app | normal, special access | exact alarm scheduling |
| `POST_NOTIFICATIONS` | app | runtime | ring and wake-check notifications |
| `USE_FULL_SCREEN_INTENT` | app | normal, special access | alarm over the lock screen |
| `FOREGROUND_SERVICE` | app | normal | ringing service |
| `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | app | normal | media playback type |
| `WAKE_LOCK` | app | normal | screen and CPU during ring |
| `RECEIVE_BOOT_COMPLETED` | app | normal | reschedule after reboot |
| `VIBRATE` | app | normal | vibration |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | app | normal, Play-restricted | optional battery exemption prompt |
| `CAMERA` | app | runtime | barcode challenge |
| `ACTIVITY_RECOGNITION` | app | runtime | step challenge |
| `ACCESS_NETWORK_STATE` | ML Kit | normal | connectivity status only |
| `INTERNET` | ML Kit | normal | **removed** from the merged manifest |
| `com.wecker.app.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` | androidx.core | signature | non-exported dynamic receivers |

## Data inventory

| Data | Where | Protection |
| --- | --- | --- |
| Alarm time, weekdays, date, label, challenge settings, barcode value, ringtone URI | Room `wecker.db`, app-private | Android app sandbox, no backup, no device transfer, no network |
| Theme, language, custom color, snooze, vibration | DataStore `settings`, app-private | same |
| Signing key | `keystore/` outside the repo | gitignored, never committed |
| ML Kit telemetry | third-party transport | cannot upload without `INTERNET` |

The app code writes no logs of alarm data and has no analytics SDK of its own.

## Network

- `grep` over `app/src` finds no `java.net`, `javax.net`, `SSL*`, `HttpsURLConnection`, OkHttp or Retrofit usage.
- The release manifest has no `INTERNET` permission, so no socket can be opened, by app code or libraries.
- `ACCESS_NETWORK_STATE` remains because ML Kit's transport calls `ConnectivityManager`; removing it risks a `SecurityException`. It exposes connectivity status only.
- Android TLS is Conscrypt/the platform provider, not Oracle JSSE.

## Components

| Component | Exported | Notes |
| --- | --- | --- |
| `MainActivity` | yes | Launcher only. Reads no intent extras. |
| `RingActivity` | no | Full-screen alarm, excluded from recents. |
| `AlarmService` | no | Media playback foreground service. |
| `AlarmReceiver` | no | Exact alarm trigger. |
| `WakeCheckReceiver` | no | Wake check and final dismissal. |
| `BootReceiver` | no | Reschedules after boot. |
| `MetadataHolderService` (CameraX) | no | Disabled. |
| `MlKitComponentDiscoveryService` (ML Kit) | no | Component discovery. |
| `MlKitInitProvider` (ML Kit) | no | Authority `com.wecker.app.mlkitinitprovider`. |
| `InitializationProvider` (androidx.startup) | no | Authority `com.wecker.app.androidx-startup`. |
| `GoogleApiActivity` (GMS) | no | — |
| `MultiInstanceInvalidationService` (Room) | no | — |
| datatransport services/receivers | no | `JobInfoSchedulerService` additionally guarded by `BIND_JOB_SERVICE`. |
| `ProfileInstallReceiver` (profileinstaller) | yes | Guarded by `android.permission.DUMP`. |

No WebView, no content provider beyond the two init providers above, no file provider.

## Static analysis

### Android Lint (`lintRelease`)

0 errors, 18 warnings:

- 13 × `GradleDependency` (outdated libraries, S-10)
- 2 × `InlinedApi` for `ACTION_REQUEST_SCHEDULE_EXACT_ALARM` and `ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT`. Both call sites sit behind SDK checks (`Build.VERSION.SDK_INT < 31` / `< 34`), so they cannot run on older versions.
- 1 × `BatteryLife` for the battery-optimization exemption (S-8)
- 1 × `UnusedResources` (`ic_launcher_background` color, superseded by the drawable)
- 1 × `ObsoleteSdkInt` (`mipmap-anydpi-v26` with minSdk 26)

### DEX string scan

One `classes.dex`, scanned for dangerous API references:

- WebView / `setJavaScriptEnabled`: none
- `Runtime.exec` / `ProcessBuilder`: none
- `DexClassLoader`: none
- `Cipher` / `KeyStore`: none
- `SmsManager`, `LocationManager`, `MediaRecorder`, `AudioRecord`: no app usage
- Accessibility service, notification listener, device admin: none
- Single references to `PathClassLoader`, `MediaRecorder` and `PackageInstaller` exist inside library code, not in app code.

### Native libraries

Four ABIs (`arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64`) with `libbarhopper_v3.so` and `libimage_processing_util_jni.so` (ML Kit barcode), `libandroidx.graphics.path.so`, `libdatastore_shared_counter.so`. Third-party native code is trusted as shipped by its vendor and not audited here.

## Dependency scan

- 117 resolved artifacts from `releaseRuntimeClasspath`.
- OSV.dev batch query (`ecosystem: Maven`) returned **0 vulnerabilities**.
- Pipeline sanity check: `log4j-core:2.14.1` returned 7 entries and `protobuf-javalite:3.21.6` returned 4, so the query path works.
- Coverage limit: OSV's Maven index covers Maven Central. Artifacts published only to Google Maven (androidx, ML Kit, GMS) are not fully indexed. Lint's `GradleDependency` warnings are the fallback for currency.
- Latest available versions reported by lint: Compose BOM 2026.09.00, activity-compose 1.13.0, lifecycle 2.11.0, navigation 2.10.2, Room 2.8.5, DataStore 1.3.0, CameraX 1.6.2, desugar 2.1.5.

## Secrets

- `git grep` for `api_key`, `apikey`, `secret`, `token`, `password`, `AIza` and private-key headers: only a documentation sentence about "secrets in the cryptographic sense".
- `git log --all --name-only` contains no `.jks`, `keystore`, `local.properties`, `.apk` or `.aab`.
- The release keystore lives outside the repository in `keystore/`, gitignored.

## Reviewed CVEs

### CVE-2026-91403 (Oracle Java SE / GraalVM, JSSE)

**Not affected.** The advisory covers Oracle Java SE 8u511, 11.0.33, 17.0.21, 21.0.13, 25.0.5, 27.0.1 and GraalVM for JDK 17.0.21 / 21.0.13 and Enterprise 21.3.20. The flaw sits in Oracle's JSSE.

- The APK contains DEX only, no Java runtime.
- No `javax.net.ssl` or `java.security` usage in app code; Android uses Conscrypt.
- The build JDK (Temurin 17.0.20.1) is build-only, not shipped, and accepts no inbound TLS connections. Adoptium's newest 17 build at review time predates Oracle's 17.0.21 CPU, so a future Temurin update is still advisable for the build machine.

## Residual risks

1. Outdated dependencies (S-10). No known CVEs in the resolved set, but the versions are a year or more behind.
2. Unencrypted local database (S-6). Accepted for app-private storage.
3. ML Kit telemetry library remains in the APK. It cannot transmit without `INTERNET`, but it keeps `ACCESS_NETWORK_STATE` and native code.
4. No dynamic testing. This audit is static; it does not replace a device or emulator test, and it does not cover the third-party native libraries.

## Recommendations

1. Update dependencies in stages (Compose BOM + Kotlin first, then CameraX/ML Kit, then Room/DataStore) and re-run lint plus this audit.
2. Re-check Adoptium for a Temurin 17 build newer than 17.0.20.1 and update `.toolchain/jdk`.
3. Keep `allowBackup=false`, `fullBackupContent=false` and `dataExtractionRules` when changing the data layer.
4. If the app ever ships on Google Play, remove `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`.
5. Keep the signing key outside the repository and signed releases at v2/v3.

## Out of scope

- Android OS and vendor images.
- Attacks that require root or physical access to an unlocked device.
- Vendor native libraries (ML Kit, CameraX, AndroidX).
- Google Play policy compliance beyond the permission note above.
