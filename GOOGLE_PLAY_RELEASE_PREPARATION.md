# TableSnap — Google Play release preparation

## Current implementation audit (source review)

- Package: `com.tablesnap.app`
- Version: `1.0.0` (versionCode 1)
- Target SDK: 36
- No app account, advertising, billing, social features, location feature, or developer-operated backend is implemented in the reviewed source.
- User-selected images are read through Android's system content picker. OCR is performed by bundled Google ML Kit Text Recognition SDKs.
- The app writes CSV/XLSX files to a user-selected destination through Android's document picker.
- The app now includes an in-app Privacy Policy link to https://tranloy51.github.io/TableSnap/.

## Important SDK privacy finding

The bundled ML Kit Text Recognition SDKs process the image and recognized text on-device and do not send image content or OCR output to Google servers. However, Google's ML Kit disclosure documentation says ML Kit sends limited diagnostics and usage metrics to Google over HTTPS. These include device information, app/package/version information, per-installation identifiers, feature events, API configuration, input/output sizes, performance metrics, and error codes. Google says this SDK metrics data is not transferred to third parties.

References:
- https://developers.google.com/ml-kit/terms
- https://developers.google.com/ml-kit/android-data-disclosure
- https://support.google.com/googleplay/android-developer/answer/10787469

## Data safety form — preparation notes, not a submitted declaration

Based on the current source and Google's ML Kit disclosure guide:

- Do not declare selected images or OCR text as collected by TableSnap: they are processed locally and not transmitted off-device.
- The app does have SDK telemetry collection through ML Kit. The form should not be answered as "no data collected" without accounting for this.
- Candidate data types to map in Play Console:
  - Device or other IDs: ML Kit per-installation identifiers.
  - App info and performance: diagnostics/performance metrics and error codes.
  - Review whether ML Kit feature events need to be mapped to App interactions under the exact current Play Console wording.
- Collection purpose: analytics/diagnostics (use the exact purpose choices offered in Play Console).
- Sharing: Google's ML Kit disclosure says this collected SDK metrics data is not transferred to third parties. Confirm the current SDK disclosures before submitting.
- Encryption in transit: ML Kit says collected metrics are encrypted in transit using HTTPS.
- Deletion: the app has no user account or developer-side database of user images/table content and currently has no in-app deletion request mechanism for SDK diagnostics. Review the exact question and current SDK retention/deletion controls before final submission.

The developer is responsible for the final declaration. This note is a source-based preparation aid, not a legal determination.

## Content rating — expected questionnaire responses

The app is a productivity/utility tool for scanning table text from images and exporting spreadsheets. Based on the reviewed functionality:
- No violence, sexual content, profanity, controlled substances, gambling, or user-to-user communication features are implemented.
- No ads, in-app purchases, or location-based features are implemented.
- User-entered and OCR-derived content stays in the user's editable local table and is not published or shared through the app.
- Suggested category: Utility / Productivity (choose the closest category actually offered).
- Answer the IARC questionnaire truthfully based on the app as shipped; review the calculated rating before submitting.

## Store assets

- App icon: 512 × 512 PNG.
- Feature graphic: 1024 × 500 PNG.
- Phone screenshots: 1080 × 1920 PNG, showing the actual app UI.
- Store listing draft: see `PLAY_STORE_LISTING.md`.

## Remaining Play Console actions

1. Upload/confirm the current signed AAB after the in-app privacy link change.
2. Add the icon, feature graphic, and screenshots to the main store listing.
3. Complete Data safety using the final SDK disclosure mapping.
4. Complete the IARC content rating questionnaire.
5. Review app access, target audience, ads declaration, privacy policy URL, and all Play Console warnings.
6. Submit for review only after the developer reviews and confirms all declarations.
