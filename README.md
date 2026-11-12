# 🚗 Velocímetro Automotivo v2.0 - High-Precision GPS Speedometer & RPM Reactive Neon Frame

[![Download APK](https://img.shields.io/badge/Download_APK-VelocimetroAutomotivo.apk-00F0FF?style=for-the-badge&logo=android)](https://github.com/lokaovida-eng/EagleUnoApp/raw/main/VelocimetroAutomotivo.apk)
[![Version](https://img.shields.io/badge/Version-2.0.0-00FF66?style=for-the-badge)](https://github.com/lokaovida-eng/EagleUnoApp)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-7F52FF?style=for-the-badge&logo=kotlin)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=for-the-badge&logo=android)](https://developer.android.com/jetpack/compose)

**Velocímetro Automotivo** is an enterprise-grade, high-precision Android GPS telemetry and trip monitoring application. Engineered with **100% window edge-to-edge transparency**, **low-latency haptic feedback**, and a **speed-reactive RPM neon border frame** (inspired by Formula 1 shift lights), Velocímetro Automotivo delivers a modern, high-contrast HUD driving experience.

---

## 🚀 Key Features

### ⚡ RPM Speed-Reactive Neon Border Frame
- **Vivid LED Shift Bar (`#00FFCC` ➔ `#00FF66` ➔ `#FFFFCC00` ➔ `#FFFF0055`)**: Triggers when accelerating, pulsing dynamically in real-time proportional to vehicle speed and rate of acceleration.
- **Dynamic Speed Readout**: Ultra-fast digital speed text with 3D neon outline glow.
- **Edge-to-Edge Display**: Full screen without black margins on Samsung Galaxy Z Flip 5 / 6 cover display and standard smartphones.

### 📱 Samsung Galaxy Z Flip Cover Display & Home Widgets
- **Foldable Cover Screen Native Integration**: Full support for Samsung Galaxy Z Flip 5 / 6 cover display widgets via Samsung Good Lock / MultiStar framework.
- **Real-Time Zero-Click Widgets**: Instant synchronization across home screen and cover screen widgets while driving (Speed, Distance KM, Trip Time).

### ⏱️ Low-Latency GPS Telemetry & Trip Monitoring
- **High-Precision GPS Synchronization**: Configured for 100ms ultra-low latency updates with noise filtering for immediate speedometer response.
- **Automatic Trip Counter**: Tracks active driving time and cumulative trip distance (`KM`), pausing automatically when idling.

---

## 📥 Direct APK Download

Download the pre-compiled **VelocimetroAutomotivo.apk** directly to your Android device:

👉 **[Download Velocímetro Automotivo APK (VelocimetroAutomotivo.apk)](https://github.com/lokaovida-eng/EagleUnoApp/raw/main/VelocimetroAutomotivo.apk)**

---

## 🛠️ Tech Stack & Specifications

| Feature | Technology |
| :--- | :--- |
| **Language** | Kotlin `1.9.22` |
| **UI Framework** | Jetpack Compose `2024.02.00` (Material 3) |
| **Min SDK** | `26` (Android 8.0 Oreo) |
| **Target SDK** | `34` (Android 14) |
| **Location Services** | Android High-Precision LocationManager API |
| **Build System** | Gradle `8.5` |

---

## 📁 Repository Structure

```text
VelocimetroAutomotivo/
├── VelocimetroAutomotivo.apk           # Compiled Direct Installer
├── app/
│   ├── build.gradle.kts
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml
│           ├── java/com/eagleuno/app/
│           │   ├── MainActivity.kt               # Jetpack Compose HUD UI & RPM Neon Logic
│           │   ├── NormalWidgetProvider.kt       # Home Screen Widget Provider
│           │   └── CoverScreenWidgetProvider.kt  # Z Flip Cover Screen Widget Provider
│           └── res/
│               ├── layout/                       # AppWidget XML Layouts
│               └── values/                       # Themes & Strings
├── build.gradle.kts
└── README.md
```

---

## 👨‍💻 Developer & License

Developed by **lokaovida-eng / Velocímetro Automotivo Team**.  
Licensed under the [MIT License](LICENSE).
