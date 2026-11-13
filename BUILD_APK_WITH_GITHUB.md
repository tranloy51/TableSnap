# Build the TableSnap APK in the cloud (no Android Studio)

This project includes a GitHub Actions workflow at `.github/workflows/build-apk.yml`.

1. Create a new empty GitHub repository.
2. Upload the contents of this folder to the repository (the `.github` folder must be included).
3. Open the repository's **Actions** tab.
4. Select **Build TableSnap APK** and click **Run workflow**.
5. Wait for the workflow to finish. If it succeeds, open that run and download the **TableSnap-debug-apk** artifact.
6. Extract the downloaded artifact ZIP. It contains `app-debug.apk`. Send it to your Android phone and install it.

If the build fails, open the failed workflow run, copy the red error section from the build log, and send it back so the source can be corrected. No Android Studio or local Android SDK is needed on your computer.

This is a debug APK for testing, not a signed Google Play release. A signed AAB will be prepared separately for Play Console after device testing.
