# TableSnap — Android MVP

TableSnap is a native Android app that converts screenshots/photos containing tables into editable rows and exports them as `.xlsx` or `.csv`.

## Included in this MVP

- Native Kotlin + Jetpack Compose UI, phone-first and responsive to larger screens.
- Android photo picker (`GetContent`) for image import.
- On-device Google ML Kit text recognition: Latin, Chinese, Devanagari, Japanese and Korean scripts.
- Basic geometry-based row/cell reconstruction from OCR word bounding boxes.
- Editable cell preview, add-row/add-column controls and row deletion.
- Local XLSX generation (minimal Office Open XML workbook) and UTF-8 CSV export.
- No TableSnap-operated server, account system, advertising or billing is implemented. The bundled ML Kit SDK processes images and OCR text on-device, but sends limited diagnostics and usage metrics to Google over HTTPS; see the privacy policy and ML Kit disclosure guide.
- App name localized for English, Simplified Chinese, Malay and Indonesian based on Android system locale.

## Open and run

1. Install Android Studio (current stable) and a JDK 17.
2. Open this `TableSnap` folder in Android Studio.
3. Allow Android Studio to sync Gradle and download dependencies.
4. Connect an Android device (Android 7.0 / API 24 or newer) or start an emulator.
5. Press **Run**.

To create a release bundle, configure a signing key in Android Studio and use **Build → Generate Signed Bundle / APK → Android App Bundle**.

This source package does not include a prebuilt APK/AAB or Gradle wrapper JAR. Android Studio can import the Gradle project and use its configured Gradle distribution. A real release build and device QA must be completed before Play Console submission.

## Important MVP limitations

- Table reconstruction is heuristic. It groups OCR words by vertical position and horizontal gaps; complex merged cells, borderless tables, rotated images, multi-page tables and dense financial reports need more advanced layout detection.
- Latin OCR covers English, Malay, Indonesian and many other Latin-script languages. Other scripts are selectable where the corresponding ML Kit model is included.
- OCR output must be reviewed by the user before export. The app does not claim perfect recognition.
- Exported XLSX cells are deliberately written as text to avoid treating untrusted OCR content as formulas. Numeric/date type inference is not included yet.
- The app has no ads or billing yet. Those should be added only after product testing and after completing Google Play disclosures/policy checks.
- No automated Android build was possible in the authoring environment because Android SDK/Gradle are not installed. Build and device verification remain outstanding.

## Privacy

The MVP processes the selected image and OCR text locally using bundled ML Kit recognizers. ML Kit may send limited SDK diagnostics and usage metrics to Google over HTTPS, but does not send the image content or recognized text to Google servers. Android's system photo picker grants access only to the selected item. The app does not operate an account or backend service, and does not include ads or billing. See the privacy policy and Google ML Kit data disclosure guide for details.

## Suggested QA before publishing

- Test on at least one low/mid-range Android phone and one tablet.
- Test clear screenshots, compressed screenshots, skewed photos, dark mode images, multi-line cells and tables with missing values.
- Open exported XLSX files in Microsoft Excel and Google Sheets; verify UTF-8 CSV with Chinese and other scripts.
- Verify app behavior with no network connection.
- Add app icon, screenshots, store listing, support contact, public privacy policy URL and signed release bundle.
