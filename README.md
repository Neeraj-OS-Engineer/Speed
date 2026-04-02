# 📱 Ultra-Native Android Template (2026 Edition)

![Platform](https://img.shields.io/badge/Platform-Android%20SDK-brightgreen?style=for-the-badge&logo=android)
![Build](https://img.shields.io/badge/Build-Gradle%208.1-blue?style=for-the-badge)
![UI](https://img.shields.io/badge/UI-Fullscreen%20Stealth-black?style=for-the-badge)

Ye ek **High-Performance Android Template** hai jo bina kisi external library (No AndroidX, No Material) ke design kiya gaya hai. Iska maqsad developers ko ek aisi "Clean Slate" dena hai jahan wo apna AI logic ya Game engine zero-error ke saath build kar sakein.

---

## 💎 Template Ki Khasiyat (Realistic Features)

* **🚫 Zero Dependency:** Ismein koi `androidx` ya `google.material` nahi hai. Build size 1MB se bhi kam rehta hai.
* **🌑 Stealth Dark Mode:** Default theme pure black (`#000000`) hai jo OLED screens par battery bachata hai aur premium look deta hai.
* **🖥️ True Fullscreen:** Navigation buttons (Back/Home) aur Status bar automatically hidden rehte hain (**Immersive Mode**).
* **⚡ High-Speed Build:** Kam files aur zero libraries hone ki wajah se GitHub Actions ise sirf 1-2 minute mein build kar deta hai.
* **🛠️ Ready-to-Fork:** Poora folder structure (app, gradle, manifests) pehle se set hai, bas apna logic daalein aur APK taiyar.

---

## 📁 Project Directory Structure

```text
ROOT_FOLDER/
├── .github/workflows/      # 🚀 GitHub Actions (Auto APK Build System)
├── app/
│   ├── src/main/
│   │   ├── java/com/demo/app/MainActivity.java  # Main Logic Control (Pure Java)
│   │   ├── res/
│   │   │   ├── layout/activity_main.xml       # UI Design (Pure Android XML)
│   │   │   └── values/strings.xml             # App Strings
│   │   └── AndroidManifest.xml                # System Permissions & Fullscreen Theme
│   └── build.gradle        # Simple Gradle Config (No Dependencies)
├── gradle/wrapper/         # Gradle Wrapper System Files
└── build.gradle            # Project Root Config
