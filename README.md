# 🦅 Eagle Uno - Translucent Speedometer & Dynamic Reactive Neon Frame

![Version](https://img.shields.io/badge/Version-1.4-00F0FF?style=for-the-badge)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-7F52FF?style=for-the-badge&logo=kotlin)
![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=for-the-badge&logo=android)
![License](https://img.shields.io/badge/License-MIT-green?style=for-the-badge)

**Eagle Uno** is a modern, ultra-sleek Android application designed for real-time GPS speed tracking and trip distance monitoring. Featuring a **100% translucent glass UI** that reflects the user's desktop or wallpaper, and an intelligent **speed-reactive dynamic neon border frame**, Eagle Uno delivers an unparalleled visual experience for drivers and enthusiasts.

---

## ✨ Key Features & Highlights

### 🚀 Real-Time GPS Tracking
- **High-Precision Speedometer**: Real-time velocity measurement in `KM/H`.
- **Automatic Distance Calculator**: Tracks cumulative trip distance in kilometers (`KM`) with reset controls.

### 🪟 100% Translucent Glass UI
- **Window Transparency**: Completely eliminates solid black backgrounds, reflecting the user's device background, wallpaper, or active cover screen seamlessly.
- **Glassmorphism Aesthetic**: Frosted glass card containers for crystal-clear readability under any lighting condition.

### 🌈 Dynamic Speed-Reactive Neon Frame (Visual Feedback)
- **Standard Threshold (> 30 km/h)**: Activates a vibrant, semi-transparent ambient green neon border frame.
- **Rapid Acceleration Mode**: Automatically triggers a high-speed **PC-Gamer RGB Rainbow Cycle** animation when accelerating rapidly.
- **Deceleration / Braking Mode**: Dynamically transitions to a **Vivid Baby Blue Neon** (`#00F0FF`) visual feedback when slowing down or braking.

### 📲 Auto-Updating Android Widgets
- **Real-Time Synchronized Widgets**: Supports Home Screen and Cover Screen Widgets (`NormalWidgetProvider` and `CoverScreenWidgetProvider`).
- **Zero-Click Updates**: Automatically updates speed (`KM/H`) and distance (`KM`) on the widget in real-time as the vehicle moves—no manual tapping required.

### 💖 Creator Page & Support
- **Integrated PayPal Support**: Includes a direct, seamless donation button (`https://www.paypal.com/donate/?hosted_button_id=REPB3R2VW4AVA`) to support ongoing app development.

---

## 🛠️ Tech Stack & Specifications

| Component | Specification |
| :--- | :--- |
| **Language** | Kotlin `1.9.22` |
| **UI Framework** | Jetpack Compose `2024.02.00` (Material 3) |
| **Min SDK** | `26` (Android 8.0 Oreo) |
| **Target SDK** | `34` (Android 14) |
| **Build System** | Gradle `8.2+` / Android Gradle Plugin `8.2.2` |
| **Architecture** | Reactive Compose State Flow & Android Location APIs |

---

## 📁 Repository & File Structure

```text
EagleUnoApp/
├── app/
│   ├── build.gradle.kts
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml
│           └── java/com/eagleuno/app/
│               ├── MainActivity.kt               # Main Jetpack Compose UI & GPS Logic
│               ├── NormalWidgetProvider.kt       # Home Screen Widget Provider
│               └── CoverScreenWidgetProvider.kt  # Cover Screen Widget Provider
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

---

## 🚀 How to Build & Launch (Developer Guide)

1. **Clone or Open Project**:
   ```bash
   git clone https://github.com/lokaovida-eng/EagleUnoApp.git
   ```
2. **Open in Android Studio**:
   - Launch Android Studio and click **Open**.
   - Navigate to the project directory: `/home/deck/Projects/EagleUnoApp`.
3. **Build APK**:
   - Go to **Build ➔ Build Bundle(s) / APK(s) ➔ Build APK(s)**.
   - The compiled `app-debug.apk` will be generated in `app/build/outputs/apk/debug/`.

---

## 👨‍💻 Creator & License

Developed with ❤️ by the **Eagle Uno Team**.  
Licensed under the [MIT License](LICENSE).
