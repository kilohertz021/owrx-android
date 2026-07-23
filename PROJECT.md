# SignalDeck Project

## Purpose

SignalDeck is a mobile-first Android client for OpenWebRX-compatible SDR receivers. The project goal is to keep the live OpenWebRX waterfall and audio engine, while replacing desktop-oriented controls with a compact phone-friendly deck.

## Current Status

Status: public Android APK test build.

Latest published APK:

```text
https://kilohertz021.org/signaldeck/SignalDeck-latest.apk
```

Current public version:

```text
0.1.26
```

Versioned APK:

```text
https://kilohertz021.org/signaldeck/SignalDeck-0.1.26-82f0459.apk
```

Repository:

```text
https://github.com/kilohertz021/owrx-android
```

## Product Principles

- Keep the waterfall and receiver state visible.
- Prefer native Android controls for repeated mobile actions.
- Avoid copying OpenWebRX or other SDR app UI concepts directly.
- Keep `kilohertz_sdr` excellent, but support compatible public OpenWebRX receivers.
- Avoid hiding native OpenWebRX decoder output.
- Use versioned APK filenames for every shared build.

## Scope

In scope:

- Android WebView client for OpenWebRX-compatible receivers.
- Mobile tuning deck with rotary encoder.
- Receiver list loaded from `rx-tx.info`.
- Receiver/mode controls optimized for phones.
- App/OpenWebRX volume control and guarded power-off.
- Basic styling and layout cleanup over OpenWebRX pages.
- Debug APK publishing for testers.

Out of scope for now:

- Google Play production release.
- Native SDR DSP/audio engine.
- Full reimplementation of OpenWebRX.
- Guaranteed compatibility with every public OpenWebRX fork.

## Current Risks

- OpenWebRX pages vary between servers and forks.
- WebView can be fragile when switching between incompatible receivers.
- Android may throttle WebView audio/WebSockets in the background.
- Decoder panels are owned by OpenWebRX and may not expose consistent markup.

## Definition of Done

For app changes:

- Debug APK builds with `.\gradlew.bat assembleDebug`.
- Basic tuning still changes frequency.
- Receiver tab opens and closes.
- Deck tab opens and closes.
- `SDRs` list opens and `kilohertz_sdr` remains pinned.
- APK is published with a versioned filename.
- `SignalDeck-latest.apk` is updated.
- `CHANGELOG.md` is updated when behavior changes.

For releases:

- `versionCode` is incremented.
- `versionName` is incremented.
- APK filename contains version and commit hash.
- Public URL is verified.
- Git commit is pushed.

## Maintainer Workflow

1. Implement and test locally.
2. Build debug APK.
3. Commit and push.
4. Publish versioned APK.
5. Update `SignalDeck-latest.apk`.
6. Ask testers to install the versioned APK, not a cached old file.
