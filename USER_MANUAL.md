# SignalDeck User Manual

SignalDeck is an Android client for OpenWebRX-compatible SDR receivers. It is currently a test APK, not a Google Play release.

## Download

Use the latest published APK:

```text
https://kilohertz021.org/signaldeck/SignalDeck-latest.apk
```

Current published test version: `0.1.3`.

## Install

1. Download the APK on your Android phone.
2. Open the downloaded APK.
3. If Android asks for permission to install apps from this source, allow it for your browser, Telegram, or file manager.
4. Tap `Install`.
5. Open `SignalDeck`.

Android may warn that the app was not installed from Google Play. That is normal for a test APK.

## Main Screen

The app opens `kilohertz_sdr` by default. You will see the OpenWebRX waterfall, band labels, receiver frequency, and the SignalDeck control deck.

## Tuning

- Rotate the `TUNE` knob to change frequency.
- Tap the center of the knob to change the tuning step.
- The default tuning step is `1 kHz`.

## Receiver Panel

Use the right-side `Receiver` tab to open the receiver controls.

The receiver panel contains:

- mode buttons such as `FM`, `WFM`, `AM`, `LSB`, `USB`, `CW`, `DATA`, `DRM`, `DAB`, and `HDR`;
- the `DIG` decoder selector;
- waterfall level controls when the current OpenWebRX page exposes compatible controls.

Swipe the receiver panel to the right to close it.

## Deck Panel

The SignalDeck deck contains:

- live receiver/frequency information;
- the `TUNE` knob;
- `SDRs`;
- `Zoom +`;
- `Zoom -`;
- `SQ`;
- `NR`.

Swipe the deck to the right to hide it. Use the right-side `Deck` tab to bring it back.

## SDR List

Tap `SDRs` to open the receiver list.

The list is loaded from:

```text
https://rx-tx.info/map-sdr-points
```

`kilohertz_sdr` stays pinned at the top. Other public receivers may be offline, overloaded, incompatible, or temporarily broken.

## Decoder Results

OpenWebRX decoder output, such as skimmer, ISM, TPMS, FAX, or SSTV panels, is still rendered by the OpenWebRX page. SignalDeck lightly styles decoder tables when they are present, but it does not create decoded packets itself.

If the waterfall shows signals but the results table is empty, either the selected decoder did not decode anything yet, the selected mode is not appropriate for the signal, or the remote receiver did not expose decoder output in the page.

## Troubleshooting

If there is no audio:

- check phone volume;
- check Bluetooth routing;
- try another OpenWebRX mode;
- return to `kilohertz_sdr`;
- choose another SDR from `SDRs`.

If the screen is blank or white after switching receivers:

- wait a few seconds;
- return to `kilohertz_sdr`;
- restart the app if the remote receiver left WebView in a broken state.

For debug logs, long-press the SignalDeck title in the deck. The recent WebView diagnostics are copied to the clipboard.
