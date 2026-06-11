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
- Update `CHANGELOG.md`.
- Bump `versionCode` and `versionName` in `app/build.gradle` if needed.

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
