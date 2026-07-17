# Release Checklist

## Debug build

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat clean assembleDebug
```

Debug APK:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Before sharing APK

- Confirm receiver URL is correct.
- Test on emulator.
- Test on real phone.
- Check audio start, mute and volume.
- Check background behavior with screen locked.
- Check notification permission on Android 13+.
- Check receiver list loading from `rx-tx.info`.
- Check fallback receiver list by testing without network.
- Check public-positioning wording in `docs/public-positioning.md` before making the repository public.
- Update `CHANGELOG.md`.
- Bump `versionCode` and `versionName` in `app/build.gradle` if needed.
- Publish a versioned APK filename:

```text
SignalDeck-<version>-<commit>.apk
```

- Update `SignalDeck-latest.apk` only after the versioned APK is uploaded.

## GitHub

Commit source changes:

```powershell
git status --short
git add .
git commit -m "Describe change"
git push
```

Do not commit:

- `local.properties`
- `.gradle/`
- `build/`
- generated APK/AAB files
- local Codex attachments

## Local APK download server

Current LAN test drop target:

```text
D:\SignalDeck\apk
```

on Windows host:

```text
192.168.1.185
```

The helper scripts are:

```text
tools/serve-signaldeck-apk.ps1
tools/start-signaldeck-apk-server.cmd
```

Current download URLs:

```text
http://192.168.1.185:8099/
http://192.168.1.185:8099/SignalDeck-latest.apk
```

## Public APK hosting

Current external test APK location:

```text
https://kilohertz021.org/signaldeck/
```

Always share the versioned APK when testing a specific build.
