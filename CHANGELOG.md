# Changelog

## Unreleased

### Added

- Receiver deck button now toggles the Receiver panel open/closed; swipe control is no longer used
- Receiver controls now keep only SQ and NR rows; volume/audio row is hidden
- Receiver Controls section is now hidden completely in the app skin
- `kilohertz_sdr` stays pinned at the top of the SDR list even while searching
- Deck status now reports `No data` for receivers that load but do not expose usable OpenWebRX state
- Native safe-area insets keep the frequency ribbon below camera cutouts and lift the deck above rounded/nav edges
- Deck spacing increased so the live frequency no longer sits under the tuning knob
- WebView receiver switching now resets page state and allows mixed OpenWebRX content
- Deck layout now centers the live frequency and encoder with compact right-side actions
- Visible native blocker over the OpenWebRX image expander was removed
- OpenWebRX image expander and opened image/author drawer are hidden by the WebView skin
- Deck action buttons made thinner and aligned higher beside the encoder
- Native OpenWebRX Log and other non-Receiver panels are hidden in SignalDeck skin
- Deck and Receiver panels now extend to the screen edges
- Waterfall-safe Receiver handle behavior that no longer manipulates the native header drawer area
- Deck header UTC moved to its own line and receiver list action renamed back to SDRs
- Receiver open control moved to a native Android overlay so it works after the WebView panel is swiped closed
- SDRs action is aligned in the right deck button column with Zoom controls
- Receiver button moved onto the deck and deck buttons were slimmed down
- Slight corner radius restored for panels, buttons and Receiver skin controls
- Tuning knob redrawn with a darker brushed-metal encoder style
- Native OpenWebRX station-header layout is left unmanaged to preserve the waterfall
- Receiver Settings and Display cleanup hardened for alternate OpenWebRX section markup
- Panel, button and WebView skin corners changed to square 90-degree edges
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
