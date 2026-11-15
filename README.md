# CatVision — Cat Breed Detection

Android application that monitors the camera and detects **cat breeds** using
on-device **TensorFlow Lite (MobileNetV2)**. The app uses CameraX for camera
input, runs inference locally on the device, and stores detection history in
Room (SQLite).

![Kotlin](https://img.shields.io/badge/Kotlin-2.1.21-7F52FF?logo=kotlin)
![Gradle](https://img.shields.io/badge/Gradle-8.11.1-02303A?logo=gradle)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-1.8.0-4285F4?logo=jetpack-compose)
![TensorFlow Lite](https://img.shields.io/badge/TensorFlow%20Lite-2.16.1-FF6D00?logo=tensorflow)

---

## 📱 Features

- **Live camera preview** — CameraX with manual/auto-start, auto-focus,
  zoom, and torch (flash) support.
- **Cat / not-cat detection** — MobileNetV2 (ImageNet) runs on-device in
  real time; hysteresis logic prevents flickering labels.
- **Breed estimation** — estimates cat breed from visual features (color,
  pattern, color-point) plus the ImageNet top-8 hints.
- **Photo capture** — takes photos, saves them locally, and keeps them in
  the detection history.
- **Image analysis** — pick a photo from the gallery and analyze it.
- **History** — view, filter, and delete past detections (with photos).
- **Breed database** — full encyclopedia of popular and contest cat breeds
  (origin, size, weight, lifespan, temperament, traits, care).
- **Settings** — auto-start camera, haptics, detection threshold, save
  detections.

## 📋 Requirements

- **Min SDK:** 24 (Android 7.0 Nougat)
- **Target SDK:** 34
- **Compile SDK:** 36
- **Language:** Kotlin 2.1.21
- **Gradle:** 8.11.1 (via `gradlew`)

## 🚀 Build

```bash
# Debug APK
./gradlew assembleDebug

# Release APK (signed)
./gradlew assembleRelease
```

The debug APK is output to:
`cat-vision/app/build/outputs/apk/debug/app-debug.apk`

The release APK is output to:
`cat-vision/app/build/outputs/apk/release/app-release.apk`

### Device install

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
# or
adb install app/build/outputs/apk/release/app-release.apk
```

## ⚙️ Configuration

### `local.properties`

Required for local builds:
```properties
sdk.dir=C:\Users\<user>\AppData\Local\Android\Sdk
```
Point `sdk.dir` to your local Android SDK.

### Signing release builds

Create `keystore.properties` in the project root (it is **not** versioned):

```properties
storePassword=<your password>
keyPassword=<your password>
keyAlias=<your alias>
storeFile=keystore/cctv-release.jks
```

Generate a keystore with:

```bash
keytool -genkey -v \
  -storetype PKCS12 \
  -keystore keystore/cctv-release.jks \
  -alias cctv \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

> **Note:** The `keystore/` directory is git-ignored. Keep your keystore
> private — losing it means you cannot sign new releases of the same app.

## 📁 Project Structure

```
cat-vision/
├── app/
│   ├── src/main/
│   │   ├── java/id/cv/catvision/
│   │   │   ├── ui/          # MainActivity, CatVisionViewModel, Compose UI
│   │   │   ├── detector/    # CatDetector (TensorFlow Lite inference)
│   │   │   ├── breed/       # BreedEstimator, CatBreeds, CoatColor/Pattern
│   │   │   └── model/       # Room database (DetectionDao, DetectionEntry)
│   │   ├── assets/
│   │   │   └── mobilenet_v2.tflite
│   │   └── res/
│   ├── build.gradle.kts
│   └── src/main/AndroidManifest.xml
├── gradle/wrapper/
├── keystore/                  # (git-ignored) release signing keystore
├── .gitignore
└── README.md
```

## ⚡ How it works (inference pipeline)

1. **CameraX** provides a `PreviewView` + `ImageAnalysis` (YUV_420_888,
   640×640, `STRATEGY_KEEP_ONLY_LATEST`).
2. `convertYUV420ToRgb888` crops the center square and scales to **224×224**.
3. `BreedEstimator.analyze` extracts visual features
   (dominant color, pattern, color-point score, brightness/saturation).
4. `CatDetector` runs MobileNetV2 inference. It uses:
   - full-frame inference + center-zoom (62%) for stability,
   - horizontal-augmentation (mirror) + EMA smoothing for the camera,
   - hysteresis + object-label smoothing to avoid flickering.
5. When a cat is detected, `BreedEstimator.estimate` ranks breeds using the
   visual features + ImageNet hints, and the result is shown in the UI.
6. Detections are saved to Room (with optional photos) and shown in the
   history sheet.

## 🔒 Security & Privacy

- All inference runs **on-device** — no images are uploaded to a server.
- Camera permission is requested at runtime (API 23+).
- Photos are saved to `ExternalFilesDir/Pictures/CatVision` (private app
  storage).
- The model is bundled in `assets/` and copied to the app's cache on first
  launch.

## 📄 License

This project is provided as-is for personal/educational use. The model
(MobileNetV2 ImageNet) is subject to the original TensorFlow model license.
```

---

Let me write the README and push it.