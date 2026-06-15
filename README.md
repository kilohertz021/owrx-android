# SignalDeck Android

Private Android SDR receiver console focused on OpenWebRX-compatible receivers.

The current default receiver is:

```text
https://sdr.kilohertz021.org/
```

It uses Android `WebView` for receiver pages and adds an independent Android-native control layer for receiver selection and common controls.

## What is this?

This project is an early Android app for personal SDR listening:

- opens the kilohertz receiver by default
- loads OpenWebRX receiver entries from `rx-tx.info`
- provides a searchable receiver list
- enables JavaScript, WebSocket and WebAudio support through `WebView`
- adds an independent native Android control overlay
- replaces tune up/down buttons with a rotary tuning knob
- keeps the screen awake while the receiver is visible
- starts a foreground service for background playback attempts
- shows a persistent notification while the receiver is running
- uses a partial wake lock to reduce Android background sleep issues

The UI is intentionally not a copy of any existing OpenWebRX Android app. It is an independent client for compatible receiver endpoints.

## Supported setup

Tested with:

- Android Studio
- Android SDK Platform 36
- Android Gradle Plugin 8.13.0
- Gradle 8.13 wrapper
- Android emulator `Medium_Phone_API_36.1`
- `https://sdr.kilohertz021.org/`
- OpenWebRX receiver directory data from `https://rx-tx.info/map-sdr-points`

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

Open `SignalDeck` on the device and start the receiver from the loaded receiver page.

## Background behavior

Android is strict about background browser-like apps. This app includes a foreground service and wake lock, but the current WebView-based architecture may still lose the OpenWebRX+ WebSocket when Android freezes JavaScript in the background.

If stable background listening becomes the main goal, the next architecture should move receiver audio/WebSocket handling into a native foreground media service and keep WebView only as the visual control surface.

See:

```text
docs/background-playback.md
```

## Native control overlay

The app adds an Android-native overlay above the WebView:

- live frequency/status strip
- searchable OpenWebRX receiver list
- rotary tuning knob
- center tap on the tuning knob cycles tuning step
- mute
- receiver/status/log panel shortcuts
- waterfall zoom controls
- waterfall auto-range
- reload
- hide/show control panel

These controls currently call compatible receiver browser functions through JavaScript injection. This keeps the working web receiver intact while improving phone ergonomics.

## Documentation

More details:

- `docs/build.md`
- `docs/install.md`
- `docs/background-playback.md`
- `docs/troubleshooting.md`
- `docs/release.md`
- `docs/public-positioning.md`

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
- The receiver display is currently the compatible web UI inside Android WebView.
- Background playback is best-effort in the current WebView architecture.
- The receiver list is filtered to OpenWebRX entries from rx-tx.info.
- The project has been built locally, but real-phone testing is still needed.

## License

MIT License.
