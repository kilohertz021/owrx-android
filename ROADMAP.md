# SignalDeck Roadmap

## v0.1.x - Mobile Control Polish

- Stabilize Deck and Receiver side tabs.
- Keep tuning reliable across OpenWebRX forks.
- Keep SQ/NR and waterfall controls usable on phones.
- Improve decoder table readability without breaking native OpenWebRX panels.
- Keep versioned APK releases.

## v0.2.x - Decoder Experience

- Inspect ISM/TPMS, FAX, SSTV, skimmer and other decoder panels across several receivers.
- Add a safer mobile wrapper for decoded packet tables.
- Add a visible empty state when a decoder panel exists but has no decoded rows.
- Add debug diagnostics for decoder DOM state.
- Preserve native OpenWebRX output when custom styling fails.

## v0.3.x - Receiver Compatibility

- Classify public OpenWebRX receivers by compatibility.
- Mark offline/broken receivers in the SDR list.
- Add faster fallback to `kilohertz_sdr`.
- Improve receiver switching recovery after blank/white pages.

## v0.4.x - Background Playback

- Evaluate Android foreground service behavior on real devices.
- Improve notification text and controls.
- Document realistic limits of WebView background playback.

## v1.0 Candidate

- Stable mobile UI for core listening.
- Reliable `kilohertz_sdr` workflow.
- Clear public documentation.
- Known compatibility limits documented.
- Signed release build process ready.
