# Changelog

## Unreleased

### Added

- SignalDeck app naming and independent receiver-console positioning
- Live OpenWebRX receiver catalog loader from `rx-tx.info`
- Searchable receiver drawer with fallback receiver list
- Persistent last-selected receiver
- Custom rotary tuning knob replacing tune up/down buttons
- Center tap on tuning knob to cycle tuning step
- Native Android overlay above the OpenWebRX+ WebView
- Live frequency/status strip
- Quick controls for tuning, tuning step, mute, panels, zoom, waterfall auto-range and reload
- Hide/show behavior for the native control panel

## v0.1.0 - 2026-06-10

Initial private working version.

### Added

- Android WebView shell for `https://sdr.kilohertz021.org/`
- JavaScript, DOM storage, WebSocket and WebAudio-capable WebView configuration
- Fullscreen receiver view
- Back-button handling through WebView history
- Foreground service for background playback attempts
- Persistent playback notification
- Partial wake lock while the keep-alive service is running
- Android launcher and notification vector icons
- Gradle wrapper and Android Studio project structure
