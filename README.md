# 🦅 Eagle Uno v1.4 - Translucent Speedometer & Dynamic RPM Reactive Neon Frame

[![Download APK](https://img.shields.io/badge/Download_APK-Direct_18MB_Installer-00F0FF?style=for-the-badge&logo=android)](https://github.com/lokaovida-eng/EagleUnoApp/raw/main/app-debug.apk)
[![Version](https://img.shields.io/badge/Version-1.4.0-00FF66?style=for-the-badge)](https://github.com/lokaovida-eng/EagleUnoApp)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-7F52FF?style=for-the-badge&logo=kotlin)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=for-the-badge&logo=android)](https://developer.android.com/jetpack/compose)

**Eagle Uno** is a modern, high-precision Android GPS speedometer and trip monitoring application. Built with **100% window transparency**, **instant haptic button feedback**, and a **speed-reactive RPM neon border frame** (inspired by sports car shift lights), Eagle Uno provides an incredible visual driving experience.

---

## 🚀 Key Features

### ⚡ RPM Speed-Reactive Neon Border Frame (Visual Feedback)
- **Vivid Red Neon (`#FF0033`)**: Triggers when accelerating. Flashes dynamically in real-time proportional to your vehicle's speed and rate of acceleration (like Formula 1 / sports car steering wheel shift lights).
- **Vivid Baby Blue Neon (`#00F0FF`)**: Activates when decelerating or braking, pulsing synchronously with the rate of deceleration.
- **Translucent Ambient Glow**: Elegant transparent border when driving at constant speed.
- **Stopped Mode**: Border completely disables when stationary (`0 km/h`).

### 🪟 100% Translucent Glass UI
- **Window Transparency**: Fully transparent window background reflecting your device's desktop, live wallpaper, or cover screen.
- **Calibrated Readable Metrics**: High-contrast glassmorphic panels displaying speed (`KM/H`), trip distance (`KM`), and driving time (`HH:MM:SS`).

### ⏱️ Trip Timer & Distance Calibration
- **Driving Time Counter**: Automatically counts driving time while moving and pauses when stopped. Resets **ONLY** when you press "Zerar KM".
- **Calibrated GPS Distance**: High-accuracy location filtering with noise suppression for precise trip calculations.

### 📲 Real-Time Auto-Updating Widgets
- **Zero-Click Synchronization**: Updates home screen and cover screen widgets in real-time as you drive—no manual interaction required.

### 📳 Instant Haptic Feedback
- **Tactile Vibration**: Tactile response when interacting with navigation controls (`◀`, `OK`, `▶`) and menu selections.

### 💖 Creator Support & PayPal Integration
- **Direct PayPal Link**: Integrated support page with a direct PayPal donation button (`https://www.paypal.com/donate/?hosted_button_id=REPB3R2VW4AVA`).

---

## 📥 Direct APK Download

Click the badge below to download the latest **18 MB APK** directly to your device:

👉 **[Download EagleUnoApp.apk (18 MB)](https://github.com/lokaovida-eng/EagleUnoApp/raw/main/app-debug.apk)**

---

## 🛠️ Tech Stack & Specifications

| Feature | Technology |
| :--- | :--- |
| **Language** | Kotlin `1.9.22` |
| **UI Framework** | Jetpack Compose `2024.02.00` (Material 3) |
| **Min SDK** | `26` (Android 8.0 Oreo) |
| **Target SDK** | `34` (Android 14) |
| **Location Services** | Android LocationManager API |
| **Build System** | Gradle `8.5` |

---

## 📁 Repository Structure

```text
EagleUnoApp/
├── app-debug.apk                       # Compiled 18MB Direct Installer
├── app/
│   ├── build.gradle.kts
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml
│           ├── java/com/eagleuno/app/
│           │   ├── MainActivity.kt               # Jetpack Compose UI & RPM Neon Logic
│           │   ├── NormalWidgetProvider.kt       # Home Screen Widget Provider
│           │   └── CoverScreenWidgetProvider.kt  # Cover Screen Widget Provider
│           └── res/
│               ├── layout/                       # Widget Layouts
│               └── values/                       # Themes & Styles
├── build.gradle.kts
└── README.md
```

---

## 👨‍💻 Developer & License

Developed by **lokaovida-eng / Eagle Uno Team**.  
Licensed under the [MIT License](LICENSE).
