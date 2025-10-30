## compose-multiplatform-local-clipboard

Cross‑platform clipboard sharing app built with Kotlin Compose Multiplatform. One codebase, multiple targets: Android, iOS, Desktop (JVM), and Web (JS/Wasm). Sync, view history, and manage devices on your local network.

### Features
- Clipboard sync across devices on the same network
- Clipboard history with sections and search
- Device discovery and approval workflow
- Light/Dark theme, responsive UI
- Multiplatform persistence via SQLDelight

### Tech Stack
- Kotlin Multiplatform + Compose Multiplatform (Material 3)
- Ktor (client/server), Multicast discovery
- SQLDelight for storage
- Coil 3 for images

### Modules
- `sharedUI/` – shared UI, domain, data, networking
- `androidApp/` – Android entry
- `iosApp/` – iOS entry (Xcode project)
- `desktopApp/` – Desktop entry
- `webApp/` – Web entry (JS + Wasm)

### Getting Started
Prereqs: JDK 17+, Android Studio (latest), Xcode (for iOS), Node (optional for web). Gradle wrapper included.

#### Android
- Run from Android Studio or: `./gradlew :androidApp:installDebug`
- APK: `androidApp/build/outputs/apk/debug/androidApp-debug.apk`

#### Desktop (JVM)
- Run: `./gradlew :desktopApp:run`
- Hot reload: `./gradlew :desktopApp:hotRun --auto`

#### iOS
- Open `iosApp/iosApp.xcodeproj` in Xcode and run
- Or command line to device (after signing):
  - `xcodebuild -scheme iosApp -destination 'id=<DEVICE_ID>' -configuration Debug build install`
  - Launch: `xcrun devicectl device process launch --device <DEVICE_ID> com.kmpstarter.KmpStarter`

Notes: Ensure code signing is configured in Xcode (Automatically manage signing). On iOS 26 SDK, avoid Swift bindings; use platform APIs in `sharedUI/src/iosMain`.

#### Web (JS/Wasm)
- JS dev run: `./gradlew :webApp:jsBrowserDevelopmentRun`
- Wasm dev run: `./gradlew :webApp:wasmJsBrowserDevelopmentRun`
- Production distribution: `./gradlew :webApp:composeCompatibilityBrowserDistribution`
- Deploy folder: `webApp/build/dist/composeWebCompatibility/productionExecutable`

### Project Scripts
- `run-linux-mac.sh` – quick desktop run
- `test_app_status.sh` – status helper

### Build/Run Tips
- Clean: `./gradlew clean`
- If Gradle cache issues: delete `.gradle/` and `**/build/`
- If iOS build DB locks: `killall xcodebuild`

### License
MIT (update if needed)

