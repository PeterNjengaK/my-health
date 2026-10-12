# My Health

A personal, ad-free Android health journal with automatic daily steps, GPS workouts, connected health imports, and daily habit tracking. Built for Android 10 and later; Health Connect imports require Android 14 or later. Version 2.1 introduces a free core with a one-time Pro upgrade through Google Play. The development build keeps Pro tools unlocked. See [release readiness](store/release-readiness.md) for the remaining account-side launch requirements.

## Included

- **Automatic daily steps:** native counter and optional detector sensors, one-time Android activity permission, ongoing foreground service, startup/reboot/package-update recovery, local-midnight daily rollover, persistent history, editable goals, weekly charts and stride-based distance estimates. Repeated starts reuse the sensor listener. Detector timestamps help partition counter increments around midnight; counter totals remain authoritative.
- **Daily dashboard:** step progress, hydration, sleep, exercise minutes, most recent weight, medication routine, and today's journal.
- **Health journal:** water, weight, sleep duration and quality, workouts and distance, meals/calories/macronutrients, blood pressure, heart rate, blood glucose (mmol/L or mg/dL), temperature, mood, symptoms, cycle notes, medication schedules, dose adherence, and freeform notes. Records can be backdated, edited in place, and deleted.
- **Insights:** step history, goals achieved, hydration/sleep averages on logged days, nutrition totals, measurement history, and weight/heart-rate/sleep/mood trends.
- **Medication reminders:** daily Android notifications, rescheduled after restart; delete the medication entry to stop its reminder.
- **GPS workouts:** start a walk/run/ride, track duration, measured distance and pace in a foreground location service, pause/resume, finish/save, and view an offline route preview. Routes are private local data. GPS accuracy filters exclude stale, noisy and implausible fixes. Interrupted sessions are recoverable in a paused state.
- **Health Connect:** optional read-only imports for steps, sleep, exercise, weight, heart rate, blood pressure, glucose and temperature. Import the last 30 days on demand; refresh when returning to the app, up to every 15 minutes. Record IDs prevent duplicate imports; local edits and deleted-import tombstones are respected. Steps use platform aggregation, then take the larger of phone and imported totals. Habit totals compare sources to reduce double counting.
- **Food barcode scanning:** portrait camera scanner, manual barcode entry, HTTPS nutrition lookup through Open Food Facts, portion scaling, and an offline cache for previously looked-up products. Lookup failures and missing nutrition allow manual entry. Verify community data against the package label.
- **Hydration reminders:** configurable 30–240 minute intervals during 8 am–9 pm, skipping notifications after the daily water goal is met.
- **Step widget:** home-screen progress, daily goals and tracking state. Updated with steps and periodic refresh. Tap to open My Health. Counts are hidden when app-lock protection is enabled.
- **Reports:** 7/30/90-day movement calendars and weight change; CSV export of all records and daily steps; a paginated 30-day PDF report. Files are saved through Android's document picker. CSV text cells protect against spreadsheet formula injection.
- **App lock:** optional Android biometric/device-credential authentication, journal hidden when leaving the app, and screenshot protection while enabled. Fingerprints are handled by Android; the app stores no biometric data.
- **Personalization:** name, daily step/water/sleep goals, stride length, and light/dark/system appearance.
- **Backup and restore:** JSON export through Android's document picker; restore validates the file and asks before replacing current records.

## Privacy

There is no advertising SDK, analytics SDK, My Health account system, or health-data cloud backend. Records and GPS routes live in the application's private storage. Food lookup sends only the product barcode to Open Food Facts over HTTPS; no journal entries, personal measurements, Health Connect data or GPS coordinates are included. Google Play handles Pro payment processing; signed purchase receipts are verified and cached locally, and no payment card data is received by the app. Android cloud backup and device transfer are disabled. Exported backups and reports contain sensitive health data, so choose their storage location carefully.

The interface uses bundled HTML/CSS/JavaScript in a locked-down WebView. Only a local synthetic HTTPS origin is served; external navigation, WebView network loads, file access, and content access are blocked. Android Java code handles sensors, GPS, Health Connect, the barcode scanner/lookup, authentication, persistent records, notifications, widgets and file operations. User-provided text is escaped before rendering. JavaScript bridge reads and writes are denied while the app is locked. Backups validate record IDs, measurements, dates, and route coordinates before replacement.

## Getting started

1. Open **My Health** and allow Android's **Physical activity** permission once. Steps then run automatically, including after app updates and phone restarts. Each new local calendar day starts at 00:00; no daily enable/pause switch is needed. Allow notifications for ongoing progress and reminders.
2. Carry the phone while walking. The count begins from the first sensor reading after enabling tracking; it cannot recover steps taken before that baseline.
3. Set your daily goals under **You**.
4. Add health records in **Journal** or use the dashboard shortcuts.
5. If the phone pauses tracking, allow background activity in the app's battery settings. Android force-stop prevents tracking and reminders until the app is opened again.
6. Export a backup before uninstalling; uninstalling removes private app records.
7. Start GPS sessions from **Activity**, scan a food from **Today** or the meal form, and change appearance, hydration reminders, widget and app-lock settings under **You**.
8. Choose Health Connect access under **You**, then sync. A compatible watch/app must first write records to Health Connect. An empty successful sync means no source records are available; the phone does not independently measure sleep or vital signs.

## Boundaries

- A hardware step-counter sensor is required for automatic steps. This app does not infer steps from an accelerometer on unsupported devices.
- Daily totals follow the phone's local timezone rather than a rolling 24-hour timer. Counter totals are partitioned using matching detector timestamps when available. If Android stops the process or loses events across midnight, unknown increments are assigned to the counter event's day; precise missing history cannot be reconstructed. Midnight UI/notification/widget refresh alarms are inexact under Android power management, but reading today's total always selects today's bucket. Daylight-saving transitions still roll over at local 00:00.
- Daily step distance is an estimate. GPS-session distance uses accepted GPS fixes and depends on signal quality. Route previews draw the recorded path without map tiles or street labels. This version does not calculate calories burned.
- Sleep and vital signs can be entered manually or imported from Health Connect. Automatic sleep requires a compatible source device/app. Blood pressure, glucose, heart rate, and temperature require an appropriate separate measuring device. Charts are descriptive; the app provides no diagnosis or treatment advice.
- Health Connect access is limited to approved categories and the last 30 days. It does not directly pair a watch over Bluetooth or write data back. Imports refresh while the app is in the foreground; provider availability and permissions can vary by device.
- To avoid inflated daily totals, phone steps and imported steps are compared rather than added. Sleep/workout totals use the highest source total for a given day; this conservative approach can undercount distinct activities logged to different sources.
- After a reboot or force-stop, a GPS workout is recovered paused using its latest persisted duration. Background GPS depends on Android location permissions and power management. No background location permission is requested; sessions must start while the app is visible.
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

The sixteen sensor-math checks cover initial baseline, increments, duplicate/backwards events, reboot, midnight boundaries, 23/25-hour daylight-saving days and detector-assisted splitting. `scripts/test-billing.ps1` verifies valid receipt signatures, tampering, missing keys and wrong keys.

For device smoke checks, open the debug app, obtain its PID with `adb shell pidof com.peter.myhealth`, and forward its WebView debugger:

```sh
adb forward tcp:9222 localabstract:webview_devtools_remote_<PID>
node scripts/device-smoke.cjs
```

The smoke script checks all five pages and fourteen log forms, native save/edit/delete, atomic validation, themes, barcode portions, movement calendars, escaping, scroll position and JavaScript exceptions. It creates temporary records and removes them in `finally`; it never modifies step totals. Avoid interacting with the app during this test.

Native checks cover real PDF generation, CSV escaping/formula protection, valid/invalid measurements, workout timing and decoding a real EAN-13 barcode:

```sh
./gradlew :app:assembleDebugAndroidTest
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w com.peter.myhealth.test/com.peter.myhealth.IntegrationChecks
```

`node scripts/device-gps.cjs` checks start, pause, stable paused duration, resume, finish/save and cleanup on a foreground phone with GPS and precise location enabled. It skips an already-active user workout and never injects simulated locations.

Validated on a TCL T807W running Android 16: upgrade preserving records/steps, automatic/repeated service starts, hardware sensors, widget, real Open Food Facts lookup, Health Connect consent and empty sync, GPS controls, UI/persistence checks and Pro boundaries. Android lint passes with no errors. Live billing purchase/restore/refund tests require the owner's Play Console product and license testers; they have not been performed against a published product. Fingerprint authentication requires the owner's biometric/device credential. Advisory lint warnings remain for intentional persistence, bundled JavaScript, English native strings and newer SDK versions.

## Free core, Pro and release preparation

Core steps, journal, Health Connect, reminders, widget, biometric privacy, JSON backup/restore and CSV exports remain free. One-time Pro adds GPS recording, barcode nutrition lookup, 30/90-day movement calendars/trends and PDF reports. Debug builds deliberately provide a clearly labeled Pro preview; release builds require a verified purchased receipt. Missing billing configuration never charges or unlocks production Pro.

Run `scripts/create-upload-key.ps1` once and `scripts/build-release.ps1` to create the signed candidate. Private key material is under ignored `.release/`; its password is encrypted to the current Windows user. Back it up with a securely exported password as explained in [the launch guide](store/play-console-guide.md). Never commit the upload key or password.

Set `PLAY_BILLING_PUBLIC_KEY`, `PRIVACY_POLICY_URL`, `SUPPORT_EMAIL`, and `PUBLIC_DEVELOPER_NAME` before running the submission build. The public key comes from the app's Play Console licensing settings; create one-time product `my_health_pro`, choose localized prices, and validate purchase flows through Play license testing. The app cannot complete account/identity/payment setup or Google review itself.

[Release status](store/release-readiness.md), [listing copy](store/listing.md), [privacy draft](store/privacy-policy.template.md), [data-safety worksheet](store/data-safety-draft.md), and [registration/launch guide](store/play-console-guide.md) are included. Store graphics use the app's existing icon and contain no personal data. Real clean-device screenshots and the account-side requirements remain necessary before a public launch.

## Source layout

`app/src/main/java/com/peter/myhealth/` contains Android integrations, validation and step math. `app/src/main/assets/` contains the offline interface and v2 UI. `tests/`, `app/src/androidTest/` and `scripts/` contain repeatable checks. Build outputs, local SDK configuration, screenshots, backups, reports and signing keys are excluded from Git.

Implementation references: [Android Health Connect platform API](https://developer.android.com/reference/android/health/connect/HealthConnectManager), [ZXing Android Embedded](https://github.com/journeyapps/zxing-android-embedded), and [Open Food Facts API](https://openfoodfacts.github.io/openfoodfacts-server/api/). Nutrition data is attributed to Open Food Facts in the app; its database is available under ODbL.
# Version 2.2 additions

Choose kg or lbs and km or miles under **You → Daily goals & profile**. Preferences apply to weight entries, trends, step distance, GPS distance, pace and route previews; underlying records keep kg/km for consistent backups and exports. Names are entered locally, never inferred from the phone owner.

The home screen shows estimated daily walking calories: steps × stride (km) × body weight (kg) × 0.5 kcal/kg/km. Log weight or enter a profile weight first. This rough estimate excludes resting energy and other workouts, does not add GPS distances again, and follows the daily step bucket at local midnight.

