# 🚗 Velocímetro Automotivo v1.4 - High-Precision GPS Speedometer & RPM Reactive Neon Frame

[![Download APK](https://img.shields.io/badge/Download_APK-Direct_18MB_Installer-00F0FF?style=for-the-badge&logo=android)](https://github.com/lokaovida-eng/EagleUnoApp/raw/main/app-debug.apk)
[![Version](https://img.shields.io/badge/Version-1.4.0-00FF66?style=for-the-badge)](https://github.com/lokaovida-eng/EagleUnoApp)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-7F52FF?style=for-the-badge&logo=kotlin)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=for-the-badge&logo=android)](https://developer.android.com/jetpack/compose)

**Velocímetro Automotivo** is an enterprise-grade, high-precision Android GPS telemetry and trip monitoring application. Engineered with **100% window transparency**, **low-latency haptic feedback**, and a **speed-reactive RPM neon border frame** (inspired by Formula 1 shift lights), Velocímetro Automotivo delivers a modern, high-contrast HUD driving experience.

---

## 🚀 Key Features

### ⚡ RPM Speed-Reactive Neon Border Frame
- **Vivid Red Neon (`#FF0033`)**: Triggers when accelerating, pulsing dynamically in real-time proportional to vehicle speed and rate of acceleration.
- **Vivid Cyan/Baby Blue (`#00F0FF`)**: Activates during braking or deceleration, synchronizing with speed reduction rates.
- **Translucent Ambient Glow**: Subtle, elegant border frame during constant cruising speeds.
- **Idle Power Saver**: Border disables automatically when stationary (`0 km/h`).

### 📱 Samsung Galaxy Z Flip Cover Display & Home Widgets
- **Foldable Cover Screen Native Integration**: Full support for Samsung Galaxy Z Flip 5 / 6 cover display widgets via Samsung Good Lock / MultiStar framework.
- **Real-Time Zero-Click Widgets**: Instant synchronization across home screen and cover screen widgets while driving.

### ⏱️ Low-Latency GPS Telemetry & Trip Monitoring
- **5Hz GPS Synchronization**: Configured for 200ms ultra-low latency updates with noise filtering for immediate speedometer response.
- **Automatic Trip Counter**: Tracks active driving time and cumulative trip distance (`KM`), pausing automatically when idling.

### 💖 Creator Support & PayPal Integration
- **Direct Creator Support**: Integrated support modal with direct PayPal donation link (`https://www.paypal.com/donate/?hosted_button_id=REPB3R2VW4AVA`).

---

## 📥 Direct APK Download

Download the pre-compiled **18 MB APK** directly to your Android device:

👉 **[Download Velocímetro Automotivo APK (18 MB)](https://github.com/lokaovida-eng/EagleUnoApp/raw/main/app-debug.apk)**

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
├── app-debug.apk                       # Compiled 18MB Direct Installer
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
