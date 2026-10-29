# Build and install

How to build the Life Metric Android app from source and install it on an emulator or a physical device.

## Requirements

| Tool | Version | Notes |
| --- | --- | --- |
| JDK | 17 or newer | Android Studio's bundled JBR works. The Gradle build also uses the foojay toolchain resolver, so a matching JDK can be downloaded automatically. |
| Android SDK | compileSdk 37 / Build-Tools, Platform-Tools (`adb`) | Install through Android Studio → SDK Manager. |
| Gradle | 9.5.0 | Provided by the wrapper (`gradlew` / `gradlew.bat`); do not install separately. |
| Android Studio | Current stable | Optional; everything below also works from the command line. |
| Network access | Required for the first build | Gradle downloads the wrapper distribution, AGP 9.3.2, Kotlin 2.2.10, Compose BOM 2026.02.01, and other dependencies. `--offline` fails until these are cached. |

Device/emulator requirements: Android 8.0 (API 26) or newer. The app targets API 37.

## One-time setup

1. Clone or copy the project to a local directory.
2. Point Gradle at your Android SDK. Either set the `ANDROID_HOME` environment variable, or create `local.properties` in the project root:

   ```properties
   sdk.dir=C\:\\Users\\<you>\\AppData\\Local\\Android\\Sdk
   ```

   Android Studio writes this file for you when you open the project. It is git-ignored.
3. (Optional) Set `JAVA_HOME` to a JDK 17+ if `java` is not already on your `PATH`. On Windows with Android Studio installed:

   ```powershell
   $env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
   ```

## Build from the command line

Run all commands from the project root.

Windows (PowerShell):

```powershell
.\gradlew.bat :app:assembleDebug
```

macOS / Linux:

```bash
./gradlew :app:assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

Useful tasks:

| Task | Purpose |
| --- | --- |
| `:app:assembleDebug` | Debug APK, signed with the debug keystore. |
| `:app:testDebugUnitTest` | JVM unit tests (`app/src/test`), including the metrics and export/import tests. |
| `:app:assembleRelease` | Unsigned release APK (`app/build/outputs/apk/release/app-release-unsigned.apk`). See [Release builds](#release-builds). |
| `:app:bundleRelease` | Release AAB for Play Console (`app/build/outputs/bundle/release/app-release.aab`). |
| `:app:installDebug` | Builds and installs the debug APK on the single connected device. |
| `clean` | Deletes `build/` directories. |

Example: run tests and build in one go:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

The first build takes a few minutes (Gradle daemon start, dependency download, Kotlin compile). Later builds are incremental; configuration cache is enabled in `gradle.properties`.

Add `--console=plain` when capturing build output in scripts or CI.

## Build in Android Studio

1. **File → Open…** and select the project root (the folder containing `settings.gradle.kts`).
2. Wait for the Gradle sync to finish. Accept any SDK component installs it offers.
3. Choose a run target (emulator or connected device) in the toolbar and press **Run ▶** (Shift+F10). This builds, installs, and launches the debug variant.
4. **Build → Build Bundle(s) / APK(s)** produces APKs or AABs without running them.

## Install on an emulator

List available virtual devices and start one:

```powershell
$emu = "$env:LOCALAPPDATA\Android\Sdk\emulator\emulator.exe"
& $emu -list-avds
Start-Process $emu -ArgumentList "-avd","Pixel_7a"
adb wait-for-device
```

If you have no AVD, create one in Android Studio → **Device Manager → Create Device** (any phone profile, API 26+; a Google APIs image is recommended so the system file picker and share sheet have targets).

Install and launch:

```powershell
adb -s emulator-5554 install -r app\build\outputs\apk\debug\app-debug.apk
adb -s emulator-5554 shell am start -n com.markbriz.lifemetric/.MainActivity
```

`-r` replaces an existing install and keeps app data. Omit `-s emulator-5554` when only one device is connected.

## Install on a physical device

1. On the phone, enable **Developer options** (tap *Settings → About phone → Build number* seven times), then turn on **USB debugging**.
2. Connect via USB and accept the "Allow USB debugging?" prompt on the phone.
3. Confirm the device is visible:

   ```powershell
   adb devices
   ```

   The device should show as `device` (not `unauthorized` or `offline`).
4. Install:

   ```powershell
   adb install -r app\build\outputs\apk\debug\app-debug.apk
   ```

Alternatively, copy the APK to the phone and open it from a file manager. Android will ask you to allow installs from that app ("Install unknown apps") the first time.

## Release builds

Release signing is configured in `app/build.gradle.kts` and reads `keystore.properties` from the project root. That file and `*.jks` are git-ignored. Without it, `assembleRelease` still builds an unsigned APK.

1. Create a keystore once (keep it outside the repo):

   ```powershell
   $env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
   & "$env:JAVA_HOME\bin\keytool.exe" -genkeypair -v `
     -keystore "$env:USERPROFILE\life-metric-release.jks" `
     -alias lifemetric -keyalg RSA -keysize 2048 -validity 10000
   ```

2. Copy `keystore.properties.example` to `keystore.properties` and fill in the path, alias, and passwords.

3. Build:

   ```powershell
   .\gradlew.bat :app:assembleRelease
   ```

   Signed APK: `app/build/outputs/apk/release/app-release.apk`. Play Console AAB: `.\gradlew.bat :app:bundleRelease`.

You can still sign with Android Studio → **Build → Generate Signed Bundle / APK…** instead of using `keystore.properties`.

Code shrinking is currently disabled for release (`optimization { enable = false }` in `app/build.gradle.kts`).

Bump `versionCode` and `versionName` in `app/build.gradle.kts` for each release. The current values are shown on the in-app **Profile → About** screen.

## Verify the install

On first launch you should see the **Welcome** screen with the health disclaimer; scroll to the bottom, tick the checkbox, and tap **Get started**. There is no sign-in or password: the app relies on the phone's own screen lock, and all data stays on the device in the app's private storage. The bottom tabs are Home, BP, Weight, Workout, and Profile.

Upgrading from a 1.0.x build (which had local email/password accounts) keeps the data of the account that was signed in; the password is discarded.

To exercise the export/import feature from the shell, push a JSON export onto the device and pick it from **Profile → Data → Load data**:

```powershell
adb push life-metric-2026-10-16-132423.json /sdcard/Download/
```

## Troubleshooting

| Symptom | Fix |
| --- | --- |
| `Plugin [id: 'org.gradle.toolchains.foojay-resolver-convention'] was not found` | You are offline or ran with `--offline` before the plugin was cached. Connect to the network and rerun without `--offline`. |
| `SDK location not found` | Create `local.properties` with `sdk.dir`, or set `ANDROID_HOME`. |
| `Unsupported class file major version` / JDK errors | Use JDK 17+; set `JAVA_HOME` to Android Studio's `jbr` directory. |
| `adb: device 'emulator-5554' not found` | The emulator is not running, or it booted on a different port. Run `adb devices` and use the serial it prints. |
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | A build signed with a different key is installed (for example, a release build over a debug build). Uninstall first: `adb uninstall com.markbriz.lifemetric`. This deletes local app data; export it first from **Profile → Data → Export data**. |
| `INSTALL_FAILED_OLDER_SDK` | The device runs Android older than 8.0 (API 26). |
| Build is slow or runs out of memory | Increase `org.gradle.jvmargs` in `gradle.properties` (default `-Xmx2048m`). |
| Stale build output | `.\gradlew.bat clean :app:assembleDebug`. |

## Where things are

| Path | Contents |
| --- | --- |
| `app/src/main/java/com/markbriz/lifemetric/` | Kotlin source (`data/`, `domain/`, `ui/`). |
| `app/src/main/res/` | Android resources, launcher icons, FileProvider paths, backup rules. |
| `app/src/test/` | JUnit unit tests. |
| `gradle/libs.versions.toml` | Dependency and plugin versions. |
| `app/build.gradle.kts` | Application module config (SDK levels, version, dependencies). |
| `app/build/outputs/` | Built APKs and bundles (git-ignored). |
