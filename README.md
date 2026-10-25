<p align="center">
  <img src="docs/icon.png" width="120" alt="Hardwake icon">
</p>

<h1 align="center">Hardwake</h1>

<p align="center">An Android alarm clock that rings until you finish a task.</p>

Hardwake wakes you at a set time and puts a challenge between you and the stop button. Solve a few math problems, scan a barcode you registered in advance, or walk a step goal. The alarm opens full screen over the lock screen, so the notification alone will not silence it.

## Features

- **Scheduling**: one-time alarms, repeating alarms on any set of weekdays, or a single calendar date.
- **Math challenge**: 1 to 5 problems per alarm, four difficulty levels.
- **Barcode challenge**: register a barcode in the app, then scan the same one to dismiss.
- **Step challenge**: 10 to 200 steps, in tens.
- **No challenge**: a plain dismiss button, if you want the alarm without the work.
- **Wake check**: optional re-ring 5 to 30 minutes after you dismiss, repeating until you turn it off from the notification.
- **Skip next**: skip one occurrence of a repeating alarm, for a day off or a holiday, and undo it from the alarm list.
- **Snooze**: configurable from 5 to 30 minutes, available from every screen.
- **Per-alarm ringtone**: any alarm sound or audio file the system can play.
- **Settings**: language, theme mode, color scheme, custom color, snooze length, vibration.
- **Languages**: German and English, switched in settings without a restart.
- **Themes**: five color schemes, or a custom seed color picked with an HSV color picker. Light, dark, or system.

## Install

Download `Hardwake-1.2.1.apk` from the [latest release](https://github.com/thebeatmv1998/Hardwake/releases/latest).

Requires Android 8.0 (API 26) or newer. Allow your browser or file manager to install unknown apps.

## Build

You need JDK 17 and Android SDK 35.

```bash
echo "sdk.dir=/path/to/android-sdk" > local.properties

./gradlew :app:assembleDebug          # debug APK
./gradlew :app:testDebugUnitTest      # unit tests
./gradlew :app:assembleRelease        # unsigned release APK
```

The debug APK lands in `app/build/outputs/apk/debug/`. Release builds run `lintVitalAnalyzeRelease`, which downloads `lint-gradle` on the first run. Sign the release APK with `zipalign` and `apksigner` before installing it.

## Permissions

| Permission | Used for |
| --- | --- |
| `SCHEDULE_EXACT_ALARM` | ringing at the exact minute |
| `POST_NOTIFICATIONS` | the ringing and wake-check notifications |
| `USE_FULL_SCREEN_INTENT` | opening the challenge over the lock screen |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | keeping the alarm sound playing |
| `WAKE_LOCK` | turning on the screen and holding the CPU awake |
| `RECEIVE_BOOT_COMPLETED` | rescheduling alarms after a reboot |
| `VIBRATE` | vibration while ringing |
| `CAMERA` | the barcode challenge |
| `ACTIVITY_RECOGNITION` | the step challenge |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | optional prompt for systems that delay background alarms |

## Security

Hardwake's own code makes no network requests and keeps all data in app-private storage with backups disabled. ML Kit's telemetry transport declares `INTERNET`, so the build removes that permission from the merged manifest with `tools:node="remove"`; the app cannot open network connections. `ACCESS_NETWORK_STATE` remains because ML Kit reads connectivity status, which does not allow transmission. Exported components, PendingIntents, the permission set and the CVE review are documented in the [security audit](docs/security-audit.md).

## How it works

`AlarmScheduler` registers an exact alarm with `AlarmManager`. `AlarmReceiver` starts `AlarmService`, a media-playback foreground service that plays the ringtone, vibrates, and posts a full-screen notification. The notification opens `RingActivity`, which shows the configured challenge. Finishing the challenge sends `ACTION_DISMISS` back to the service, which stops the sound and either reschedules the next alarm or arms the wake check.

## Tech

Kotlin, Jetpack Compose with Material 3, Room, DataStore, CameraX and ML Kit for barcode scanning.

```
app/src/main/java/com/wecker/app/
├── alarm/      scheduling, ringing service, notifications
├── challenge/  math, barcode and step screens
├── data/       Room entities, DAO, repositories, settings
├── i18n/       German and English strings
└── ui/         alarm list, editor, settings, ring screen, theme
```

## Notes

The package id stays `com.wecker.app` so existing installs can update in place.
