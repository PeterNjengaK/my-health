# My Health

A personal, ad-free Android health journal with hardware step counting, an offline dashboard, and daily habit tracking. Built for Android 10 and later.

## Included

- **Automatic daily steps:** native `TYPE_STEP_COUNTER` sensor, physical activity permission, ongoing foreground service, daily history, editable goals, weekly charts, reboot recovery, and estimated walking distance using a configurable stride length.
- **Daily dashboard:** step progress, hydration, sleep, exercise minutes, most recent weight, medication routine, and today's journal.
- **Health journal:** water, weight, sleep duration and quality, workouts and distance, meals/calories/macronutrients, blood pressure, heart rate, blood glucose (mmol/L or mg/dL), temperature, mood, symptoms, cycle notes, medication schedules, dose adherence, and freeform notes. Records can be backdated and deleted.
- **Insights:** step history, goals achieved, hydration/sleep averages on logged days, nutrition totals, measurement history, and weight/heart-rate/sleep/mood trends.
- **Medication reminders:** daily Android notifications, rescheduled after restart; delete the medication entry to stop its reminder.
- **Personalization:** name, daily step/water/sleep goals, and stride length.
- **Backup and restore:** JSON export through Android's document picker; restore validates the file and asks before replacing current records.

## Privacy

There is no `INTERNET` permission, advertising SDK, analytics SDK, account system, or cloud backend. Records live in the application's private storage. Android cloud backup and device transfer are disabled; use the explicit export feature to move your records. Exported backups contain sensitive health data, so choose their storage location carefully.

The interface uses bundled HTML/CSS/JavaScript in a locked-down WebView. Only a local synthetic HTTPS origin is served; external navigation, network loads, file access, and content access are blocked. Android Java code handles the sensor, persistent records, notifications, boot recovery, permissions, and file operations. User-provided text is escaped before rendering.

## Getting started

1. Open **My Health** and enable step tracking. Allow **Physical activity** access and notifications when requested.
2. Carry the phone while walking. The count begins from the first sensor reading after enabling tracking; it cannot recover steps taken before that baseline.
3. Set your daily goals under **You**.
4. Add health records in **Journal** or use the dashboard shortcuts.
5. If the phone pauses tracking, allow background activity in the app's battery settings. Android force-stop prevents tracking and reminders until the app is opened again.
6. Export a backup before uninstalling; uninstalling removes private app records.

## Boundaries

- A hardware step-counter sensor is required for automatic steps. This app does not infer steps from an accelerometer on unsupported devices.
- Daily totals follow the phone's local timezone. Sensor event timestamps determine the date of delivered events. If tracking is interrupted for a long period spanning midnight, a cumulative sensor reading cannot reconstruct precisely which missing steps belonged to which day. The delta is assigned to the event's reporting day.
- Distance is an estimate. This version does not use GPS, calculate calories burned, or import wearable/Health Connect data.
- Sleep, food, workouts, and vital signs are entered manually. Blood pressure, glucose, heart rate, and temperature require an appropriate separate measuring device. Charts are descriptive; the app provides no diagnosis or treatment advice.
- Medication reminders use inexact alarms. Android power management can delay them. They are daily schedules rather than a clinical medication-management system.
- There are no fabricated starter records or simulated step totals.

## Build

Requirements: JDK 17 or newer, Android SDK platform 36, and SDK build tools. Gradle 9.4.1 and Android Gradle Plugin 9.2.1 are configured in the project.

Set `ANDROID_HOME` to your SDK, or create an ignored `local.properties` containing `sdk.dir` (escape the colon in Windows drive letters).

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:assembleDebug :app:lintDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`. This is a development build signed with the local Android debug key. Future updates must use the same signing key to install over existing records.

```sh
adb -s <phone-ip:debug-port> install -r app/build/outputs/apk/debug/app-debug.apk
```

## Verification

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/test-steps.ps1
node --check app/src/main/assets/app.js
```

The seven sensor-math regression checks cover first enable, normal increments, duplicate readings, backwards readings, reboot, and delayed events on both sides of midnight.

For device smoke checks, open the debug app, obtain its PID with `adb shell pidof com.peter.myhealth`, and forward its WebView debugger:

```sh
adb forward tcp:9222 localabstract:webview_devtools_remote_<PID>
node scripts/device-smoke.cjs
```

The smoke script checks all five pages and fourteen log forms, native persistence/deletion, escaping, validation, scroll position, and JavaScript exceptions. It creates temporary records and removes them in `finally`; it never modifies step totals. Avoid interacting with the app during this test.

Validated on a TCL T807W: install and launch, foreground health service, hardware sensor registration, and real increasing step totals. Android lint passes with no errors. Advisory lint warnings remain for JavaScript (required for bundled UI), intentional synchronous persistence, and newer available SDK versions.

## Source layout

`app/src/main/java/com/peter/myhealth/` contains Android integration and step math. `app/src/main/assets/` contains the offline interface. `tests/` and `scripts/` contain repeatable regression/device checks. Build outputs, local SDK configuration, screenshots, backups, and signing keys are excluded from Git.
