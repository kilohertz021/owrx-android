# OWRX+ Android

Private Android companion app for the `kilohertz_sdr` OpenWebRX+ receiver.

The current app is a lightweight native Android shell around:

```text
https://sdr.kilohertz021.org/
```

It uses Android `WebView` for the OpenWebRX+ interface and adds Android-side lifecycle handling for long receiver sessions.

## What is this?

This project is an early Android app for personal OpenWebRX+ use:

- opens the kilohertz OpenWebRX+ receiver directly
- enables JavaScript, WebSocket and WebAudio support through `WebView`
- keeps the screen awake while the receiver is visible
- starts a foreground service for background playback attempts
- shows a persistent notification while the receiver is running
- uses a partial wake lock to reduce Android background sleep issues

The first version intentionally stays close to OpenWebRX+ itself. The native layer is small so the receiver UI stays familiar and easy to debug.

## Supported setup

Tested with:

- Android Studio
- Android SDK Platform 36
- Android Gradle Plugin 8.13.0
- Gradle 8.13 wrapper
- Android emulator `Medium_Phone_API_36.1`
- `https://sdr.kilohertz021.org/`

The project should also run on a real Android phone with network access to the receiver.

## Quick start

### 1. Open in Android Studio

Open this directory:

```text
C:\Users\vadim\Documents\Codex\2026-06-10\owrx-android-studio-codex
```

Android Studio should detect it as a normal Gradle Android project.

### 2. Build debug APK

From the repository root:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat assembleDebug
```

The debug APK is created at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

### 3. Run

Use Android Studio Run, or install with `adb`:

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

Open `OWRX+` on the device and start the receiver from the OpenWebRX+ page.

## Background behavior

Android is strict about background browser-like apps. This app includes a foreground service and wake lock, but the current WebView-based architecture may still lose the OpenWebRX+ WebSocket when Android freezes JavaScript in the background.

If stable background listening becomes the main goal, the next architecture should move receiver audio/WebSocket handling into a native foreground media service and keep WebView only as the visual control surface.

See:

```text
docs/background-playback.md
```

## Documentation

More details:

- `docs/build.md`
- `docs/install.md`
- `docs/background-playback.md`
- `docs/troubleshooting.md`
- `docs/release.md`

## Project layout

```text
app/                         Android application module
app/src/main/java/...         MainActivity and keep-alive service
app/src/main/res/...          Android resources
gradle/wrapper/               Gradle wrapper
docs/                         Project documentation
```

## Limitations

- This is an early private working version.
- The UI is currently the OpenWebRX+ web UI inside Android WebView.
- Background playback is best-effort in the current WebView architecture.
- The receiver URL is currently hardcoded for the kilohertz SDR.
- The project has been built locally, but real-phone testing is still needed.

## License

MIT License.
