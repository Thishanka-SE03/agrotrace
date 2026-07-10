# AgroTrace Scanner Android App

A thin Kotlin Android client for the existing AgroTrace desktop-to-mobile OCR pairing workflow.

## What this app does

1. Scans an `agrotrace://ocr/claim?...` QR payload.
2. Validates `scanId`, the six-digit pairing code, and the claim token.
3. Creates a persistent random `deviceId` for the phone.
4. Claims the matching backend scan job.
5. Captures or imports one document image with ML Kit Document Scanner.
6. Uploads the image as multipart form data.
7. Reports upload success and returns the user to the scanner home screen.

The app does **not** call Gemini, perform OCR, show extracted fields, access PostgreSQL, or save pairing tokens after the workflow finishes.

## Assumed backend contract

### Claim

`POST {BASE_URL}/api/v1/ocr/scans/{scanId}/claim`

```json
{
  "deviceId": "persistent-random-device-uuid",
  "pairingCode": "256255",
  "claimToken": "token-from-qr"
}
```

Any HTTP 2xx response is treated as a successful claim. The response body may be empty or JSON.

### Image upload

`POST {BASE_URL}/api/v1/ocr/scans/{scanId}/image`

Multipart fields:

- `image`: JPEG document image
- `deviceId`: text
- `claimToken`: text

Any HTTP 2xx response is treated as an accepted upload.

If the Spring Boot controller uses different multipart names, update only `OcrScanApi.kt` and `OcrScanRepositoryImpl.kt`.

## Opening the project

1. Extract/open this folder in Android Studio.
2. Use JDK 17 for Gradle.
3. Allow Gradle to sync and download dependencies.
4. Install Android SDK 35 when prompted.
5. Run the `app` configuration on an emulator or Android phone running API 23 or newer.

The first QR or document scan can take longer because Google Play services may download its scanner module.

## Backend address

The address is editable inside the app using the settings icon.

- Android emulator: `http://10.0.2.2:8080`
- Physical phone on the same LAN: `http://YOUR_COMPUTER_IP:8080`
- Deployment: `https://your-final-server`

For a physical phone, Spring Boot must listen beyond localhost and the computer firewall must allow the selected port. Desktop and phone do not need the same LAN after the backend is deployed to a reachable HTTPS address.

The compile-time default can also be changed through the Gradle property `OCR_API_BASE_URL`. The in-app setting takes priority after it has been saved on a device.

## HTTP and HTTPS

The debug build allows cleartext HTTP for local development. The release build blocks cleartext HTTP and expects HTTPS. This is controlled by the `usesCleartextTraffic` manifest placeholder in `app/build.gradle.kts`.

## QR format

```text
agrotrace://ocr/claim?scanId=<UUID>&code=<6 digits>&token=<claim token>
```

The app also registers this custom URI as a deep link. Tapping a recognized AgroTrace link can open the pairing screen directly.

## Security choices

- No Gemini or database credentials are included.
- Claim tokens are never written to logs.
- The token remains in memory only during the active scan workflow.
- Only the backend base URL and generated device UUID are stored in DataStore.
- Release builds require HTTPS by default.
- The phone does not retrieve OCR results.

## Main source locations

- QR parsing: `domain/parser/PairingPayloadParser.kt`
- Backend URL setting: `data/preferences/ScannerPreferences.kt`
- REST endpoints: `data/remote/OcrScanApi.kt`
- Claim/upload logic: `data/repository/OcrScanRepositoryImpl.kt`
- Workflow state: `ui/ScannerViewModel.kt`
- Google scanners: `MainActivity.kt`
- Compose screens: `ui/screens/Screens.kt`

## Building the APK

Open the project in Android Studio, wait for Gradle sync, then select **Build → Build APK(s)**. Android Studio will create the debug APK under `app/build/outputs/apk/debug/`.

This source package does not include a generated Gradle Wrapper binary. Android Studio can sync the project using its configured Gradle installation; a standard wrapper can later be generated and committed from a machine with Gradle installed.
