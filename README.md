# 📝 Bloco de Notas Neon Cartoon v1.0 - Single-Screen Cartoon Notepad with Anime Eagle Mascot

[![Download APK](https://img.shields.io/badge/Download_APK-BlocoDeNotasNeonCartoon.apk-00F0FF?style=for-the-badge&logo=android)](https://github.com/lokaovida-eng/EagleUnoApp/raw/main/BlocoDeNotasNeonCartoon.apk)
[![Version](https://img.shields.io/badge/Version-1.0.0-00FF66?style=for-the-badge)](https://github.com/lokaovida-eng/EagleUnoApp)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-7F52FF?style=for-the-badge&logo=kotlin)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=for-the-badge&logo=android)](https://developer.android.com/jetpack/compose)

**Bloco de Notas Neon Cartoon** is a single-screen, cartoon-styled Android Notepad application featuring **thick cartoon neon corner borders**, **custom background theme palette selection**, and a **fierce Anime Eagle mascot** ("Águia de Anime com Cara de Mau") rendered on Canvas for idle/waiting states.

Engineered with **100% full screen Edge-to-Edge display** for Samsung Galaxy Z Flip 5 / 6 cover screen (*Flex Window*) and standard smartphones.

---

## 🚀 Key Features

### 🎨 Cartoon Neon Framing & Custom Themes
- **Thick Cartoon Corner Lines**: Custom 4-corner cartoon neon stroke framing.
- **Theme Palette Selector (`⚙`)**: Switch between Dark Cyber Neon, Cartoon Purple, Midnight Pink, Electric Cyan, and Anime Dark backgrounds.

### 🦅 Fierce Anime Eagle Mascot ("Cara de Mau")
- **Custom Canvas Anime Eagle**: Vector-rendered anime eagle head with angled glowing neon eyes, sharp beak, and fierce brow expression on empty note state.

### 📝 Single-Screen Notepad & History
- **`➕ Nova Nota`**: Instantly clears canvas for a new note.
- **`💾 Salvar`**: Saves current note title and content locally into `SharedPreferences`.
- **`📜 Histórico`**: Modal listing all saved notes with timestamps, quick note loading, and deletion.

### 📱 Samsung Galaxy Z Flip Cover Display & Home Widgets
- **Foldable Cover Screen Native Integration**: Full support for Samsung Galaxy Z Flip 5 / 6 cover display widgets via Samsung Good Lock / MultiStar framework.
- **Real-Time Synchronized Widgets**: Displays the latest saved note across home screen and cover screen widgets.

---

## 📥 Direct APK Download

Download the pre-compiled **BlocoDeNotasNeonCartoon.apk** directly to your Android device:

👉 **[Download Bloco de Notas Neon Cartoon APK (BlocoDeNotasNeonCartoon.apk)](https://github.com/lokaovida-eng/EagleUnoApp/raw/main/BlocoDeNotasNeonCartoon.apk)**

---

## 🛠️ Tech Stack & Specifications

| Feature | Technology |
| :--- | :--- |
| **Language** | Kotlin `1.9.22` |
| **UI Framework** | Jetpack Compose `2024.02.00` (Material 3) |
| **Min SDK** | `26` (Android 8.0 Oreo) |
| **Target SDK** | `34` (Android 14) |
| **Build System** | Gradle `8.5` |

---

## 📁 Repository Structure

```text
BlocoDeNotasNeonCartoon/
├── BlocoDeNotasNeonCartoon.apk        # Compiled Direct Installer
├── app/
│   ├── build.gradle.kts
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml
│           ├── java/com/eagleuno/app/
│           │   ├── MainActivity.kt               # Cartoon Neon Notepad UI & Anime Eagle Canvas
│           │   ├── NormalWidgetProvider.kt       # Home Screen Note Widget Provider
│           │   └── CoverScreenWidgetProvider.kt  # Z Flip Cover Screen Note Widget Provider
│           └── res/
│               ├── layout/                       # AppWidget XML Layouts
│               └── values/                       # Themes & Strings
├── build.gradle.kts
└── README.md
```

---

## 👨‍💻 Developer & License

Developed by **lokaovida-eng / Bloco de Notas Neon Cartoon Team**.  
Licensed under the [MIT License](LICENSE).
