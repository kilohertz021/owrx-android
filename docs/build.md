# Build

## Requirements

- Android Studio
- Android SDK Platform 36
- Android SDK Build Tools 36.x
- JDK 21 from Android Studio JBR
- Gradle wrapper included in this repository

The local SDK path belongs in `local.properties`, which is intentionally not committed.

Example:

```properties
sdk.dir=C\:\\Users\\vadim\\AppData\\Local\\Android\\Sdk
```

## Build from PowerShell

From the repository root:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat assembleDebug
```

Expected output:

```text
BUILD SUCCESSFUL
```

Debug APK:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Build from Android Studio

Open the repository root in Android Studio and use:

```text
Build > Make Project
```

or press Run with an emulator or connected phone selected.

## Common local issue

If Gradle fails with:

```text
Failed to load native library 'native-platform.dll'
```

make sure Gradle can write to the user Gradle cache:

```text
C:\Users\vadim\.gradle
```

In the Codex sandbox this may require elevated execution. In normal Android Studio usage it should not need special handling.
