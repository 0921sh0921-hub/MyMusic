# MyMusic Android

A dark-themed local music player starter project for Android. Build the APK using GitHub Actions.

## Features
- Pick one or multiple local audio files using Android's file picker
- Play/pause, previous/next, seek bar and elapsed/duration time
- Shuffle and repeat-current-track controls
- Android Equalizer and BassBoost effects (device/ROM support can vary)
- Detects Bluetooth audio output devices; Android automatically routes media audio to the selected connected Bluetooth device
- Custom MyMusic launcher icon
- GitHub Actions workflow builds a debug APK and publishes it as an artifact

## Build APK on GitHub
1. Create a new GitHub repository.
2. Upload the contents of this ZIP (the files inside the `MyMusic` folder, not the enclosing folder if you want the project at repository root).
3. Open **Actions** and allow workflows if prompted.
4. Run **Build MyMusic APK** using **Run workflow**, or push to `main`/`master`.
5. Open the completed workflow run, scroll to **Artifacts**, and download `MyMusic-debug-apk`.
6. Extract the artifact ZIP; inside is `app-debug.apk`. Install it on your Android phone.

## Notes
- This is a debug APK, not a Play Store-signed release.
- Audio effect support depends on the Android device and its audio-effect implementation.
- Bluetooth routing is handled by Android. The app detects connected Bluetooth output but does not forcibly switch routes.
- The starter uses the Android platform MediaPlayer. Playback while the app is fully closed/background playback and a media notification are not included yet.
