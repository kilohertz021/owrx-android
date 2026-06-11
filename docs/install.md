# Install

## Emulator

Use Android Studio Device Manager and start:

```text
Medium_Phone_API_36.1
```

If the emulator is unstable, use:

```text
Cold Boot Now
```

Then install from Android Studio Run, or with `adb`:

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

## Real phone

1. Enable Developer Options.
2. Enable USB debugging.
3. Connect the phone by USB.
4. Accept the debugging prompt on the phone.
5. Install:

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

Check connected devices:

```powershell
adb devices
```

If the phone shows `unauthorized`, unlock the phone and accept the USB debugging prompt.

## Notification permission

On Android 13 and newer, the app asks for notification permission. Allow it if background receiver operation is being tested.

The notification is used by the foreground service that tries to keep receiver playback alive.
