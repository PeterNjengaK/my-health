# Play policy and disclosure worksheet — review before submitting

Sources: [Google User Data policy](https://support.google.com/googleplay/android-developer/answer/10144311), [Data safety definitions](https://support.google.com/googleplay/android-developer/answer/10787469), [Health apps policy](https://support.google.com/googleplay/android-developer/answer/18258653). This is an implementation-based worksheet, not a completed declaration or a guarantee of approval.

## Data flow inventory

| Data / capability | Where it goes | Purpose |
| --- | --- | --- |
| Step history, manually logged health records, profile, mood, cycle, medication | Android app-private storage | Personal wellness tracking, goals and charts |
| Imported Health Connect records | Read from the approved local Health Connect categories into local storage | Display and summarize personal records; no writes, ads or developer upload |
| GPS coordinates | Local active/saved workout routes | User-started GPS workouts, distance, pace and route preview |
| Camera frames | Local barcode decoder only | Food barcode scanning; frames not retained/uploaded |
| Product barcode | Open Food Facts HTTPS endpoint | Optional nutrition lookup; IP address/User-Agent visible to that service |
| Purchase JSON and signature | Google Play Billing service; signed receipt cached privately | One-time Pro entitlement, acknowledgment and restoration |
| Exports | Document provider/location chosen by the user | User-directed backup and reports; exported copies may outlive app deletion |
| Fingerprint / device credential | Android biometric system; no template received by the app | Optional app lock |

There are no ads, analytics, developer-operated account or health-data backend. Google Play handles payment methods; the app does not receive card/bank details. Google explains when a payment service's processing does not require declaring the app's own collection of card data; review the conditions in the Data safety guide. Do not blanket-answer "no data collected" without reviewing SDK/service behavior and optional barcode requests under Google's definitions.

## Declarations and review evidence

- Health functionality: activity/fitness, sleep, nutrition/weight, wellness/mood, medication tracking and manually logged/imported vital signs. No diagnosis, treatment or regulated-device claim. Listing and in-app health information contain the non-medical-device disclaimer and external-measurement requirements.
- Health Connect purposes: READ_STEPS for daily/weekly steps; READ_SLEEP for sleep duration and trends; READ_EXERCISE for workout history; READ_WEIGHT for weight history; READ_HEART_RATE, READ_BLOOD_PRESSURE, READ_BLOOD_GLUCOSE and READ_BODY_TEMPERATURE for the personal measurement journal. Each category is optional; no background Health Connect read permission or medical-record APIs are requested.
- Foreground service **health**: automatic ongoing step counting with an ongoing notification. **location**: user-started GPS workout, visible recording notification with Finish action. Capture concise videos showing those real flows for Console declarations if requested.
- Revoke permissions in Android settings. Delete all health data in You. There is no server account, so the account-deletion form's account-creation branch should reflect that fact. Google Play account/payment processing is separate.
- Review complete dependency behavior: AndroidX Core 1.17.0, ZXing Android Embedded 4.3.0 and Google Play Billing 9.1.0. The app's own network calls are restricted to Open Food Facts product lookup and Play billing; WebView network loads remain blocked.
- The merged Billing dependency graph also includes Google Play services and DataTransport/encoding components. They are not a My Health health-data backend, but their presence means payment/technical SDK disclosures must be reviewed against current SDK Index guidance before completing Data safety. The merged release manifest has no advertising-ID permission. Do not infer absence of all SDK diagnostics solely from the app's own source.
- Verify the public privacy URL and contact fields, country distribution, target audience and actual test evidence before submission. The policy must be active and contain real owner/business contact information.
