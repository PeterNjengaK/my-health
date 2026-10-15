# Internal test 2.3.1 (version code 6)

This build enables Google Play Billing using the public licensing key saved locally in `.release/play-billing-public-key.txt`. The release builder loads it when `PLAY_BILLING_PUBLIC_KEY` is not already set. The key file is excluded from Git; the public key is included in the app as required for receipt verification. No private upload key or signing password is included in the app.

Product: `my_health_pro`. Buy purchase option: `pro-lifetime`. The owner has activated the purchase option in Play Console; regional prices were generated from US$4.99 and saved for 174 countries/regions. Availability and localized prices are queried from Google Play, not hardcoded.

## Release notes

Enable Google Play purchase and restore flows for the one-time My Health Pro upgrade. Internal billing test release; core health tracking remains free and ad-free.

## Test procedure

1. In the developer account's License testing settings, add the Google account used on the test phone. Being an internal tester alone does not establish license-test payment eligibility.
2. Upload the signed 2.3.1 bundle as a new internal release, review validation messages, and roll it out to the internal track.
3. Update the existing Play-installed app through Google Play. Preserve its records; uninstalling is not necessary for this update.
4. Open My Health Pro and verify that Google supplies the expected local price and buy option. Only proceed with checkout when the payment sheet explicitly identifies a test purchase/test payment method; stop if it presents an ordinary charge.
5. Test approval, cancellation, pending payment, acknowledgment, restoration after clearing/reinstalling on a separate test device, and refund/revocation refresh. Back up real records before any destructive test. Record actual outcomes; local signature tests do not replace these checks.

Public launch is still pending policy/contact URL configuration, store declarations, applicable testing and review, bank verification, and resolution of the health-app account-type question. This bundle is for internal purchase testing, not a declaration of submission readiness.
