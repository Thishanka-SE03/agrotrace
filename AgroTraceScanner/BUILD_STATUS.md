# Build and validation status

## Completed

- Complete Android Studio source project generated.
- QR payload parser validated against the supplied `agrotrace://ocr/claim` structure.
- Parser and backend URL validator exercised with standalone Kotlin checks.
- Real claim tokens are not included in source files, tests, logs, or preferences.
- Claim and multipart upload requests match the documented current assumptions.
- Debug builds permit local HTTP; release builds require HTTPS.

## Not executed in this environment

A full Android APK compilation was not possible because this execution environment does not contain the Android SDK or a local Gradle installation. The project must be synced and built once in Android Studio.

## Backend assumptions to verify during the first integration test

- Claim JSON property names: `deviceId`, `pairingCode`, `claimToken`.
- Upload multipart names: `image`, `deviceId`, `claimToken`.
- Any HTTP 2xx response means the claim/upload was accepted.

If the Spring Boot controller uses different names, only the Retrofit API and repository mapping need changes.
