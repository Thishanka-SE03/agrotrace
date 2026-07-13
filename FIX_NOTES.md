# AgroTrace 1.1 UI and Stability Update

## Fixed

- Corrected edge-to-edge layout handling so screens no longer render under the phone status bar or display cutout.
- Added navigation-bar and keyboard insets for extraction, review, and document-detail screens.
- Rebuilt the launcher icon from the supplied AgroTrace emblem with adaptive, round, legacy, and monochrome variants.
- Hardened the review-and-save flow so JSON preparation, Room insertion, and navigation failures are handled without closing the app.
- Added a dedicated global navigation action from the review screen to History after a successful save.
- Moved Room operations to the IO dispatcher and added error handling to Home, History, Profile, and Document Details.
- Fixed empty object and empty array values so manual edits are retained in the saved JSON.

## UI improvements

- Added the scanned-image preview to the review screen for side-by-side verification.
- Added a live completion indicator based on filled versus missing fields.
- Added a visible saving state and progress indicator.
- Added the supplied AgroTrace emblem to the dashboard and navigation drawer.
- Updated the app version to 1.1.0.

## Testing note

A full Android build still needs to be run in Android Studio because this export environment does not include an Android SDK. XML resources and navigation XML were parsed and validated before packaging.
