# Changelog

## Unreleased

### Added

- Receiver swipe handle moved below the frequency ribbon
- Receiver panel now opens from the handle and closes with an upward swipe
- Receiver panel hides Settings and Display sections for a cleaner app-first flow
- Expanded deck no longer shows the redundant Min button
- SignalDeck dark ice-blue palette and tighter corner radii across native controls
- SignalDeck app naming and independent receiver-console positioning
- Live OpenWebRX receiver catalog loader from `rx-tx.info`
- Searchable receiver drawer with fallback receiver list
- Persistent last-selected receiver
- Custom rotary tuning knob replacing tune up/down buttons
- Center tap on tuning knob to cycle tuning step
- Current tuning step displayed directly inside the tuning knob
- Cleaner control layout with adjacent zoom controls
- Frequency text cleanup for mixed OpenWebRX unit labels
- Top native overlay removed so OpenWebRX menus and bookmark labels stay visible
- Receiver deck can be minimized and restored from a compact bottom bar
- Deck controls reduced to tuning knob and zoom controls
- HTTP OpenWebRX receivers enabled via Android cleartext traffic setting
- SignalDeck WebView skin for OpenWebRX top controls and receiver panel
- Swipe-right gesture to close the OpenWebRX receiver panel
- OpenWebRX top action buttons hidden in SignalDeck skin
- Station header kept as the only top bar element
- Swipe down from the station header opens Receiver panel
- Swipe up on Receiver panel closes it
- Receiver panel repositioned as a top drawer over the waterfall area
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
