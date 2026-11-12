# Simple Player Android v1.0

A lightweight Android-only video player built with Kotlin and AndroidX Media3 (ExoPlayer).

## Features
- Android only; minimum Android 6.0 (API 23)
- Clean dark interface with purple accent
- Initial screen has only a centered `+` button
- Open local videos using Android's system document picker
- Default playback speed is **1.5×**
- One speed button switches only **1.5× ↔ 1.75×**
- Volume is set to 100% by default; no volume slider
- Timeline with current and total time
- Tap the video to Play/Pause
- `.srt` and `.vtt` subtitle files
- `CC+` loads a subtitle; `CC` toggles loaded subtitles on/off
- Subtitles are displayed in the middle of the video
- Fullscreen switches to landscape immersive mode
- Android Back exits fullscreen first
- No ads, login, analytics, API key, or network permission in the app

## Build APK without Android Studio (GitHub Cloud Build)

You only need a GitHub account and a web browser. GitHub's cloud runner builds the APK.

1. Download and extract this project ZIP on your computer.
2. Sign in to [GitHub](https://github.com/).
3. Create a new repository. You can name it `simple-player-android`. Choose Public or Private.
4. Open the new repository and choose **Add file → Upload files**.
5. Upload the *contents* of the extracted `SimplePlayerAndroid` folder (including the `.github` folder). Do not upload only the ZIP file.
6. Commit the uploaded files to the `main` branch.
7. Open the repository's **Actions** tab.
8. Select **Build Simple Player APK** from the left side. If it is not visible, refresh the page after the first commit.
9. Click **Run workflow → Run workflow**.
10. Wait for the green check mark. Open the completed workflow run.
11. In the **Artifacts** section at the bottom, download `Simple-Player-Android-APK`.
12. Extract the downloaded artifact ZIP. It contains `app-debug.apk`. Transfer that APK to your Android phone and install it.

The included workflow file is `.github/workflows/build-apk.yml`. It installs Java, Android SDK and Gradle on GitHub's runner, builds the debug APK, and publishes it as a downloadable workflow artifact. GitHub Free accounts have usage limits for Actions; a small Android build is usually within the free allowance.

## Build locally (optional)
If Android Studio is installed, open this folder in Android Studio and choose **Build → Build Bundle(s) / APK(s) → Build APK(s)**.

## Playback notes
Media3 uses Android's supported media decoders. MP4/H.264/AAC is widely supported. MKV, HEVC, AV1, WebM and some audio combinations depend on the phone's installed/hardware decoders. DRM-protected content is not bypassed.

## Controls
- Tap the video: Play/Pause
- Tap the single speed button: 1.5× ↔ 1.75×
- Tap `CC+`: load an SRT/VTT subtitle
- Tap `CC`: toggle loaded subtitles
- Tap fullscreen: landscape immersive mode
- Android Back: exit fullscreen

## License
MIT. See `LICENSE`.
