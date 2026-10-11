# Yawmiyati Android App

Arabic-first daily-work marketplace for Gaza. Kotlin/Compose, Material 3.

## Prerequisites

- Android SDK Platform 35
- JDK 17 (Homebrew: `/usr/local/Cellar/openjdk@17/...`)
- A running Yawmiyati Django backend (see `../backend/`)

## Build

```bash
cd android
export JAVA_HOME="/usr/local/Cellar/openjdk@17/17.0.18/libexec/openjdk.jdk/Contents/Home"
export ANDROID_HOME="$HOME/Library/Android/sdk"
./gradlew assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

## Run on emulator

```bash
./gradlew installDebug
adb shell am start -n com.yawmiyati.app/.MainActivity
```

## API configuration

Debug builds point to `http://10.0.2.2:8000/api/v1/` (Mac host from emulator).

Start the backend:
```bash
cd ../backend
source .venv/bin/activate
python manage.py runserver 0.0.0.0:8000
```

## Architecture

- **Package**: `com.yawmiyati.app`
- **UI**: Jetpack Compose + Material 3 + Arabic RTL (`android:supportsRtl="true"`)
- **Navigation**: Navigation Compose
- **State**: ViewModel + StateFlow
- **Network**: Retrofit 2.11 + OkHttp 4.12 + Gson
- **Auth storage**: EncryptedSharedPreferences (AES-256-GCM)
- **Async**: Kotlin Coroutines 1.8
- **Build**: AGP 8.5.2, Gradle 8.7, Kotlin 2.0.20, compileSdk 35, minSdk 26
