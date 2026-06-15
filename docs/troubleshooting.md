# Troubleshooting

## App opens but receiver does not load

Check network access to:

```text
https://sdr.kilohertz021.org/
```

Also verify that the Android emulator or phone has working internet access.

## WebSocket reconnects in the OpenWebRX+ log

This usually means Android paused or throttled the WebView, or the network connection changed.

Current mitigation:

- foreground service
- persistent notification
- wake lock
- WebView timer resume

If reconnects still happen in the background, the next fix is a native receiver/audio service.

See:

```text
docs/background-playback.md
```

## Receiver list does not load

SignalDeck loads OpenWebRX entries from:

```text
https://rx-tx.info/map-sdr-points
```

If the request fails, the app falls back to a small local receiver list including `kilohertz_sdr`.

Check:

- Android emulator or phone has internet access
- `rx-tx.info` is reachable
- receiver search text is not filtering out all entries

## No audio

Check:

- OpenWebRX+ page was started by tapping the start overlay
- receiver is not muted
- emulator or phone media volume is up
- Android did not block audio focus
- network speed is non-zero in the OpenWebRX+ status panel

## Notification does not appear

On Android 13 and newer, allow notification permission when prompted.

If permission was denied:

```text
Android Settings > Apps > OWRX+ > Notifications
```

Enable notifications and restart the app.

## Emulator crashpad_handler.exe error

This is an Android Emulator crash reporter issue, not an app crash.

Known workaround:

- close the error dialog
- use `Cold Boot Now`
- reinstall or update Android Emulator from Android Studio SDK Manager
- try software graphics if GPU acceleration is unstable

## adb shows unauthorized

Unlock the emulator or phone and accept the USB debugging prompt.

Then run:

```powershell
adb devices
```

The device should show as:

```text
device
```
