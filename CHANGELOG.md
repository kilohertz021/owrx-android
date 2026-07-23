# Changelog

## Unreleased

### Changed

- Kept the receiver panel above OpenWebRX banners and moved waterfall controls higher on receivers with large mode-button grids.
- Mark empty decoded packet tables as decoder cards immediately so the placeholder state matches populated tables.
- Removed the inner decoded packet card frame so table content sits directly on the outer decoded block.
- Made the decoder header a single continuous strip by removing per-cell backgrounds that created seams.
- Matched decoder table surfaces to the card background so residual table frames disappear visually.
- Removed remaining decoder table borders from native and SignalDeck-rendered packet tables.
- Side-tab strokes now leave the screen-edge side open, and decoder tables are centered without internal grid borders.
- Removed the remaining rounded screen-edge corners from side tabs and simplified decoder table grid lines.
- Added compact >>> close hints to Deck and Receiver, squared side-tab screen edges, and centered decoder card padding.
- FT8/WSJT decoded packet cards now preserve the OpenWebRX Message/Text column instead of truncating tables to four columns.
- Suppressed repeated hash-navigation diagnostics and debounced decoder DOM refreshes during tuning.
- Removed the MAP deck button after testing because it made the mobile flow bulky.
- Decoded packet panels are aligned back to the same near-edge width as Receiver and Deck.
- Receiver and Deck side tabs are thinner with tighter rotated labels.
- Decoded tables are fixed in place inside their panel and no longer allow horizontal dragging.
- Side-tab labels now use rotated text again, making the Receiver and Deck tabs more compact.
- Deck and Receiver side tabs now share one native drawing/positioning path while keeping separate panel behavior.
- Receiver opening still uses the restored Receiver WebView panel flow instead of the reverted Deck-native path.
- Fax and SSTV image decoder output now use a taller media viewer treatment instead of the compact packet-card styling.
- Restored the previous Receiver side-tab behavior instead of forcing it into the native Deck tab path.
- Fax decoder output now gets a taller dedicated viewing area instead of the compact generic decoder-card treatment.
- Receiver keeps extra bottom room below waterfall controls to avoid accidental slider touches.

### Added

- Project documentation added: `PROJECT.md`, `ROADMAP.md`, architecture notes and GitHub templates.

## v0.1.3 - 2026-07-18

### Changed

- Deck layout now emphasizes the rotary encoder with a larger tuning knob.
- Deck action buttons were made smaller and moved into a compact right column.
- Receiver button was removed from the deck; the Receiver side tab is the single Receiver entry point.
- Deck and Receiver side tabs no longer show arrow glyphs.
- Deck gesture handling is limited to the actual deck panel/tab so Receiver swipes no longer open Deck.

## v0.1.2 - 2026-07-17

### Added

- Deck tab redrawn as a dedicated side handle.
- Receiver button removed from the deck because the Receiver side tab now owns that action.
- Decoder tables are lightly styled when OpenWebRX exposes compatible table output.
- Versioned APK filename publishing introduced.

## v0.1.1 - 2026-07-17

### Added

- Global deck hide/show gestures added.
- Version code/name bumped for installable APK updates.

## v0.1.0 - 2026-06-10

### Added

- Receiver deck button now toggles the Receiver panel open/closed; swipe control is no longer used
- Temporary debug logging added; long-press the deck title to copy recent WebView diagnostics
- SQ and NR buttons added to the deck under Zoom controls
- SQ and NR deck controls changed from plain buttons to mobile-friendly range bars backed by the original OpenWebRX controls
- SQ range now drives the OpenWebRX `sql` hash parameter; NR lookup checks the hidden OpenWebRX NR panel
- Deck layout now reserves a separate right column for action buttons and SQ/NR sliders to avoid overlap with the tuning knob
- Receiver switching now recreates the WebView so broken remote SDR pages cannot leave stale white/blank layers behind
- Receiver controls now keep only SQ and NR rows; volume/audio row is hidden
- Receiver Controls section is now hidden completely in the app skin
- `kilohertz_sdr` stays pinned at the top of the SDR list even while searching
- Deck status now reports `No data` for receivers that load but do not expose usable OpenWebRX state
- Native safe-area insets keep the frequency ribbon below camera cutouts and lift the deck above rounded/nav edges
- Bandplan/skimmer/info OpenWebRX panels are no longer hidden by the SignalDeck skin
- Receiver panel height increased and fixed so its trimmed controls fit without small vertical scrolling
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
- Android WebView shell for `https://sdr.kilohertz021.org/`
- JavaScript, DOM storage, WebSocket and WebAudio-capable WebView configuration
- Fullscreen receiver view
- Back-button handling through WebView history
- Foreground service for background playback attempts
- Persistent playback notification
- Partial wake lock while the keep-alive service is running
- Android launcher and notification vector icons
- Gradle wrapper and Android Studio project structure
