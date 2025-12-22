# 🔒 Device Masker

**Elite Android Identifier Spoofing LSPosed Module**

[![Android](https://img.shields.io/badge/Android-8.0%2B-green.svg)](https://developer.android.com)
[![API](https://img.shields.io/badge/API-26%2B-brightgreen.svg)](https://android-arsenal.com/api?level=26)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![LSPosed](https://img.shields.io/badge/LSPosed-1.10.2%2B-orange.svg)](https://github.com/LSPosed/LSPosed)

Device Masker is a production-grade LSPosed/Xposed module that **spoofs device identifiers** while maintaining a sophisticated **anti-detection layer**. It features a modern Material 3 Expressive UI and uses a robust multi-module architecture for maximum stability.

---

## ✨ Key Features

### 🛡️ Anti-Detection (Stealth First)
- **Stack Trace Filtering**: Sophisticated removal of Xposed/YukiHookAPI frames from stack traces.
- **Class Hiding**: Blocks detection via `Class.forName()` for all framework classes.
- **Service Protection**: Secure AIDL-based IPC that doesn't leak to target apps.
- **proc/maps Protection**: Hides module libraries from memory maps.

### 🎭 Spoofing Groups
- **Independent Configurations**: Create multiple spoofing "Groups" for different use cases.
- **Per-App Assignment**: Assign different groups to different apps (e.g., "Banking Group", "Privacy Group").
- **Group Enable/Disable**: Master switch per group for quick control.

### 📱 Device Profile Presets
- **Hardware Identity**: Realistic predefined profiles for popular devices (Pixel 8 Pro, Samsung S24 Ultra, OnePlus 12, etc.).
- **Consistent Fingerprints**: Synchronized Model, Manufacturer, Brand, and Build Fingerprint to prevent hardware mismatch detection.

### 🧩 Intelligent Correlation
- **SIM-Location Sync**: Automatically generates matching Timezone, Locale, and GPS coordinates based on the selected SIM carrier's country.
- **Realistic Generators**: Luhn-valid IMEI/ICCID, hardware-derived Serials, and brand-compliant WiFi SSID patterns.
- **GPS City Bounds**: Valid coordinates within 42+ major cities across 16+ countries.

---

## 🏗️ Architecture

Device Masker uses a high-performance 3-module architecture inspired by HMA-OSS:

- **`:app`**: Material 3 Expressive UI, `ConfigManager` state management, and `ServiceClient` proxy.
- **`:xposed`**: High-performance hooker logic running in target processes. Anti-detection always loads first.
- **`:common`**: Shared AIDL interfaces, `@Serializable` data models, and logic-heavy value generators.

---

## 📥 Installation

### Requirements
- **Android 8.0 - 16** (API 26-36)
- **Rooted device** with Magisk/KernelSU/APatch
- **LSPosed** 1.10.2+ (Zygisk version recommended)

### Steps

1. **Install LSPosed framework** and ensure it's active.
2. **Install Device Masker APK**.
3. **Enable in LSPosed Manager**:
   - Find Device Masker in the Modules tab.
   - Enable the module.
   - **Recommended Scope**: Select "System Framework" and any target apps.
   - **⚠️ CAUTION**: Do NOT select Device Masker itself in the scope.
4. **Configure in App**:
   - Open Device Masker.
   - Navigate to the **Groups** tab to create a profile.
   - Use the **Apps** tab to select apps and assign them to your Group.
5. **Reboot** (Required for the first time or when changing scope).

---

## 🏗️ Build from Source

### Prerequisites
- **Android Studio Ladybug** (2024.1+) or newer
- **JDK 25** (Host) & **JDK 21** (Target)
- **Gradle 9.1.0**

### Build Commands

```bash
# Clone
git clone https://github.com/astrixforge/devicemasker.git
cd devicemasker

# Sync & Build Debug
./gradlew assembleDebug

# Install to device
./gradlew installDebug
```

---

## 🔧 Technology Stack

| Component | Library | Version |
|-----------|---------|---------|
| **Core** | Kotlin | 2.2.21 |
| **Framework** | YukiHookAPI | 1.3.1 |
| **Reflection** | KavaRef | 1.0.2 |
| **UI** | Compose Material 3 | 1.5.0-alpha11 |
| **Persistence** | DataStore / JSON | 1.2.0 / 1.9.0 |

---

## ⚠️ Disclaimer

This module is for **educational and security research purposes only**. 

- Do NOT use to bypass security measures illegally.
- Users are solely responsible for compliance with local laws.
- The developers assume no liability for misuse or damage caused by this module.

---

## 🤝 Contributing

We welcome contributions! Please follow the conventional commit standard and ensure all code matches the project's architecture patterns.

Made with ❤️ by **AstrixForge**
