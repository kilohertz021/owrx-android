# SignalDeck Android

SignalDeck is an experimental Android app for listening to OpenWebRX-compatible SDR receivers.

It opens `kilohertz_sdr` by default, shows the waterfall and current frequency, and adds a mobile-friendly control deck with a rotary `TUNE` knob, receiver mode selection, zoom controls, and a searchable SDR list.

## Current Test APK

Versioned APK:

```text
https://kilohertz021.org/signaldeck/SignalDeck-0.1.3-825221e.apk
```

Latest APK:

```text
https://kilohertz021.org/signaldeck/SignalDeck-latest.apk
```

This is a debug/test build for direct sharing. It is not a Google Play release.

## Features

- opens OpenWebRX-compatible SDR receivers;
- shows the receiver waterfall and frequency scale;
- provides a rotary `TUNE` control for frequency changes;
- changes tuning step with a center tap on the knob;
- opens receiver modes through the right-side `Receiver` tab;
- hides and restores the native control deck through the right-side `Deck` tab;
- opens the SDR directory through `SDRs`;
- loads OpenWebRX receiver entries from `rx-tx.info`;
- keeps `kilohertz_sdr` pinned at the top of the SDR list;
- provides mobile SQ/NR sliders and waterfall level controls;
- respects camera cutouts, rounded screen corners, and Android navigation areas.

## Installation

This is a test build and is not distributed through Google Play yet.

If someone sends you the APK file:

1. Download the APK on your Android phone.
2. Open the downloaded file.
3. If Android asks to allow installation from this source, allow it for Telegram, your browser, or your file manager.
4. Tap `Install`.
5. Open `SignalDeck`.

Android warnings about installing apps outside Google Play are normal for test APK builds.

## Basic Use

- `TUNE` - rotate the knob with your finger to change frequency.
- Center tap on `TUNE` - change the tuning step.
- `Receiver` - open receiver modes such as `FM`, `AM`, `USB`, `LSB`, `CW`, and others.
- `SDRs` - open the receiver list.
- `Zoom +` / `Zoom -` - zoom the waterfall in or out.
- Green/yellow labels on the frequency scale are OpenWebRX bandplan/skimmer hints.

Not every public SDR in the list is stable. Some receivers may be offline, overloaded, incompatible, or may not expose usable receiver state. In that case SignalDeck may show `No data`.

## Why This Repository Is Public

This repository is public so users can inspect the source code and verify what the APK is doing.

SignalDeck is an independent Android client for OpenWebRX-compatible receivers. It is not an official OpenWebRX app and is not a copy of any commercial Android SDR client.

## Developer Notes

This is a normal Gradle Android project.

Quick debug build:

```powershell
.\gradlew.bat assembleDebug
```

Debug APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

More documentation:

- [PROJECT.md](PROJECT.md)
- [ROADMAP.md](ROADMAP.md)
- [USER_MANUAL.md](USER_MANUAL.md)
- [docs/build.md](docs/build.md)
- [docs/install.md](docs/install.md)
- [docs/background-playback.md](docs/background-playback.md)
- [docs/troubleshooting.md](docs/troubleshooting.md)
- [docs/release.md](docs/release.md)
- [docs/public-positioning.md](docs/public-positioning.md)

## Status

Experimental test version. The main use case is mobile access to `kilohertz_sdr` and other OpenWebRX-compatible receivers.

Background playback is currently best-effort because Android may throttle WebView JavaScript/WebSocket activity when the app is sent to the background.

## License

MIT License.
