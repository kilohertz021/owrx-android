# Public Positioning

SignalDeck should be presented as an independent Android client for compatible SDR receiver endpoints.

## Naming

Use:

```text
SignalDeck
```

Avoid naming the app as if it is the official Android app for OpenWebRX or OpenWebRX+.

Acceptable wording:

```text
Compatible with OpenWebRX receiver pages.
```

Avoid wording like:

```text
Official OpenWebRX app
OpenWebRX Android
OpenWebRX+ mobile
```

## Design

The Android UI should use its own visual language:

- native receiver drawer
- SignalDeck top strip
- rotary tuning control
- compact Android control deck
- original iconography and colors

Do not copy:

- OpenWebRX web CSS or images
- third-party Android app layouts
- third-party icons or branding
- screenshots as app assets without permission

## Code

Current implementation opens compatible receiver web UIs and controls them through JavaScript injection.

If code from OpenWebRX/OpenWebRX+ is copied or modified in the future, its license obligations must be reviewed before publishing.

## Receiver Directory

The app loads OpenWebRX entries from:

```text
https://rx-tx.info/map-sdr-points
```

If this becomes a public feature, add visible attribution and a fallback strategy so the app remains useful when the directory is unavailable.
