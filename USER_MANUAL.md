# SignalDeck User Manual

SignalDeck is an Android client for OpenWebRX-compatible SDR receivers. It opens the receiver page, keeps the live waterfall/audio, and adds a mobile control deck for common phone actions.

## Download

Latest APK:

```text
https://kilohertz021.org/signaldeck/SignalDeck-latest.apk
```

Current public test version: `0.1.26`.

## Install

1. Download the APK on your Android phone.
2. Open the downloaded APK.
3. If Android asks for permission to install apps from this source, allow it for your browser, Telegram, or file manager.
4. Tap `Install`.
5. Open `SignalDeck`.

Android may warn that the app was not installed from Google Play. That is normal for a direct APK test build.

## Main Screen

The app opens `kilohertz_sdr` by default. The top area is the OpenWebRX receiver view with waterfall, frequency scale, and band labels. The SignalDeck panel sits over the lower part of the screen and contains the main mobile controls.

![SignalDeck main deck](docs/images/signaldeck-deck.png)

## Deck Controls

- `TUNE`: rotate the knob with your finger to change frequency.
- Center tap on `TUNE`: change the tuning step.
- `SDRs`: open the public receiver list.
- `Zoom +` / `Zoom -`: zoom the waterfall in or out.
- `SQ`: adjust squelch when the current receiver exposes compatible controls.
- `NR`: adjust noise reduction when the current receiver exposes compatible controls.
- `VOL`: adjust app/OpenWebRX audio independently from the phone volume.
- Power button: hold for 3 seconds to close SignalDeck completely.

While the tuning knob is being rotated, nearby controls are temporarily guarded from accidental touches.

Swipe the deck to the right from the `>>>` area to hide it. Use the right-side `Deck` tab to bring it back.

## Receiver Panel

Use the right-side `Receiver` tab to open receiver controls.

![SignalDeck receiver panel](docs/images/signaldeck-receiver.png)

The receiver panel can include:

- receiver/source selector;
- mode buttons such as `FM`, `WFM`, `AM`, `LSB`, `USB`, `CW`, `SAM`, `DATA`, `DRM`, `DAB`, and `HDR`;
- `DIG` decoder selector;
- waterfall level controls.

Swipe the receiver panel to the right from the `>>>` area to close it.

## SDR List

Tap `SDRs` to open the receiver list.

![SignalDeck SDR receiver list](docs/images/signaldeck-sdr-list.png)

The list is loaded from:

```text
https://rx-tx.info/map-sdr-points
```

`kilohertz_sdr` stays pinned at the top. Other public receivers may be offline, overloaded, incompatible, or temporarily broken.

## Decoder Results

OpenWebRX decoder output, such as FT8/WSJT, skimmer, ISM, TPMS, FAX, or SSTV panels, is still produced by the receiver page. SignalDeck only adapts the display for mobile.

For FT8/WSJT-style tables, SignalDeck keeps the important OpenWebRX fields visible, including message text when the server exposes it.

If the results table is empty, wait for the next decode cycle or check that the selected mode and signal match the decoder.

## Troubleshooting

If there is no audio:

- raise the phone volume;
- check `VOL` in the SignalDeck deck;
- check Bluetooth routing;
- try another OpenWebRX mode;
- return to `kilohertz_sdr`;
- choose another SDR from `SDRs`.

If a public receiver opens with a blank or broken screen:

- wait a few seconds;
- choose another SDR;
- return to `kilohertz_sdr`;
- restart the app if the remote receiver left WebView in a bad state.

For debug logs, long-press the SignalDeck title in the deck. Recent WebView diagnostics are copied to the clipboard.
