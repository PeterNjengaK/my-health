# Release status — My Health 2.1

## Prepared and validated locally

- Automatic steps after activity permission: no app enable/pause switch, startup/reboot/update recovery, persistent daily buckets changing at local midnight, detector-assisted date attribution and preserved historical totals.
- Free core and one-time non-consumable Pro flow (`my_health_pro`), Play Billing 9.1.0, localized price query, purchase-state/signature validation, acknowledgment and restoration. Production guards on GPS, barcode lookup and PDF generation; debug preview remains unlocked. Pending or unverified receipts never grant ownership.
- Private RSA upload key with Windows-encrypted password; signed Android App Bundle candidate; debug APK for the owner's existing install. Release disables WebView debugging and Pro preview.
- Adaptive launcher icon, monochrome notification icon, 512px store icon, 1024×500 feature graphic, listing draft, privacy-policy template, data-flow/declaration worksheet and account-registration guide.
- Local data-deletion control, health/medical disclaimer, account-free review access, backup/restore and user-directed exports.
- Debug/release build and Android lint checks. Sixteen midnight/DST/step-math checks, five signed-receipt/tampering checks, native automatic/repeated-start tests and existing app/device checks.

## Required before a public Google Play launch

1. Owner creates/verifies the Play Console developer account and payments profile. This project has not registered an account or paid a fee.
2. Confirm public developer name, monitored support email, legal/business details, target countries, audience and local Pro prices.
3. Publish the completed privacy policy at an active public HTTPS URL; set release contact/policy variables.
4. Configure/activate `my_health_pro`, set the app's Google Play public licensing key, upload to a test track and test real Play license-test purchase flows, pending/cancel/restore/acknowledgment and refunds. Local cryptographic tests do **not** verify the live store/payment integration.
5. Capture real screenshots from a clean test device, complete policy declarations and content rating, and test the supported Android/device matrix plus Play pre-launch reports. Personal health screenshots in `artifacts/` are excluded from Git.
6. Complete any required closed test (new personal accounts generally require 12 opted-in testers continuously for 14 days), apply for production access, and obtain Google review approval.

The candidate is **not yet submission-ready or approved**. The `-Submission` build mode rejects missing policy/contact/billing settings, but it cannot certify account completion, purchase behavior, policy declarations, tester participation or Google approval. Account-side preparation is the next step with the owner.

Purchase verification is local RSA signature verification and foreground Play purchase refresh. It is not a developer-operated server or real-time refund backend; an offline device may retain cached access until it reconnects. A server-based verification/revocation service is a potential later hardening step.
