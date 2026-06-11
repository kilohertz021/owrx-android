# Background Playback

## Current implementation

The app currently uses OpenWebRX+ inside Android WebView.

To improve behavior when the app goes to the background, the native shell starts `PlaybackKeepAliveService`, which:

- runs as a foreground service
- declares `mediaPlayback` foreground service type
- shows a persistent notification
- holds a partial wake lock
- keeps WebView timers resumed from `MainActivity`

This helps Android treat the app as an active receiver rather than an idle browser page.

## Known limitation

OpenWebRX+ audio and waterfall data are controlled by browser JavaScript and WebSocket connections. Android may still throttle or freeze WebView JavaScript when the activity is backgrounded.

Typical symptom:

```text
WebSocket has closed unexpectedly. Attempting to reconnect...
```

If this happens, the foreground service is still running, but the WebView page itself has been suspended or disconnected.

## Next architecture

For reliable background listening, move the receiver stream out of WebView:

1. Native foreground media service owns the OWRX+ WebSocket connection.
2. Native audio pipeline decodes and plays receiver audio.
3. WebView remains the visual UI and tuning/control surface.
4. Activity and service share receiver state.

That is a bigger step, but it is the correct direction if background operation becomes the core feature.

## Practical testing checklist

- Start OWRX+ audio in the app.
- Confirm the foreground notification appears.
- Lock the screen for 30 seconds.
- Unlock and check if waterfall/audio continued.
- Repeat with the app switched to another foreground app.
- Check whether the OpenWebRX+ log shows WebSocket reconnects.
