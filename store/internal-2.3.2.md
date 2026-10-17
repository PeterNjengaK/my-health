# Internal test 2.3.2 (version code 7)

Adds developer-issued reviewer access so Google can inspect the same Pro features without an account, payment, trial, OTP or second device. The visible **You → My Health Pro → Review access** form accepts a non-expiring RSA/SHA-256 signed grant. Native entitlement checks verify the grant independently of JavaScript; granting review access never marks a Play purchase as owned or replaces a purchase receipt. Purchase restoration/refunds remain independent of the grant.

Private reviewer instructions and the complete code are in `.release/reviewer-instructions.txt`. Use these in Play Console **Sign in details** (formerly App access), with **Yes** for restricted functionality. Leave username/password blank: the app has no login. Do not commit the code or private signing key, publish them on the website, or put them in release notes. Provide access instructions to Google only after the matching build is available to review.

`scripts/create-review-access.cjs` creates the private signing key once and regenerates the matching public key, signed grant and instructions. Preserve the signing key for future builds. The release builder reads `.release/review-access-public-key.txt` or `REVIEW_ACCESS_PUBLIC_KEY`. The APK includes only the public verification key; other developers cannot issue grants without the private key. A leaked valid grant can be reused, so keep the code restricted to review access. It has no hidden feature behavior: it unlocks the same Pro features available to purchasers and is clearly labeled as review access.

The native privacy screen now links to the published HTTPS policy and public support email. The release builder supplies the published policy URL and Console brand/support details. The open Pro sheet refreshes when purchase state changes instead of requiring close/reopen.

## Validation

- Eight JVM signature/purpose/tampering/missing-key checks.
- Nine device checks using an isolated `.reviewcheck` debug package: invalid grant rejected, valid grant activates, purchase ownership remains false, review UI and purchase-sheet refresh behave correctly.
- Review grant survives process restart in the isolated package. Temporary package removed; owner's Play app and records preserved.
- Five existing receipt-verification checks passed. Debug and release build/lint results are recorded in the local release logs.
- Final Play-distributed 2.3.2 update still needs confirmation after internal rollout. Existing 2.3.1 purchase/restore-refresh tests passed earlier; recovery after a clean install, pending/cancel/refund cases and wider device checks remain separate launch checks.

## Release notes

Add developer-issued review access for Pro tools without payment. Add the public privacy-policy link and refresh the Pro purchase screen automatically when access changes. Core health tracking remains free and ad-free.
