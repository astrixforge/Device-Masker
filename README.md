# 🔒 Device Masker

**Device Identifier Spoofing LSPosed Module**

[![Android](https://img.shields.io/badge/Android-10%2B-green.svg)](https://developer.android.com)
[![API](https://img.shields.io/badge/API-29%2B-brightgreen.svg)](https://android-arsenal.com/api?level=29)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![LSPosed](https://img.shields.io/badge/LSPosed-1.10.2%2B-orange.svg)](https://github.com/LSPosed/LSPosed)

Device Masker is an open-source LSPosed/Xposed module that **spoofs device identifiers** and provides a robust **anti-detection layer** to prevent apps from detecting hook injection. Designed for privacy-conscious users and security researchers.

---

## ✨ Features

### 🔧 Device Spoofing
- **IMEI/MEID** - Spoof device identifiers with Luhn-valid values
- **Serial Number** - Randomized hardware serial
- **Android ID** - Per-app or global Android ID spoofing
- **SIM Serial (ICCID)** - Fake SIM card identifiers
- **IMSI** - Subscriber identity spoofing

### 📶 Network Spoofing
- **WiFi MAC Address** - Randomized unicast MAC
- **Bluetooth MAC Address** - Hardware address spoofing
- **SSID/BSSID** - Network name spoofing
- **Carrier Info** - Operator name and MCC/MNC

### 📣 Advertising Spoofing
- **Advertising ID** - Google advertising identifier
- **GSF ID** - Google Services Framework ID
- **Media DRM ID** - Widevine device ID

### 🏗️ System Spoofing
- **Build Properties** - Model, Manufacturer, Brand, Fingerprint
- **System Properties** - ro.* property values
- **API Level** - Android version spoofing

### 📍 Location Spoofing
- **GPS Coordinates** - Latitude/Longitude
- **Timezone** - Device timezone
- **Locale** - Language settings

### 🛡️ Anti-Detection
- **Stack Trace Filtering** - Hides Xposed frames from stack traces
- **Class Loading Protection** - Blocks Class.forName() detection
- **/proc/maps Filtering** - Hides LSPosed libraries
- **Package Hiding** - Hides Xposed packages from PackageManager

---

## 📱 Screenshots

| Home | Spoof Settings | Apps | Profiles |
|------|---------------|------|----------|
| Module status, quick stats | Edit spoof values | Select apps to protect | Manage profiles |

---

## 📥 Installation

### Requirements
- Android 10+ (API 29+)
- Rooted device with Magisk 26.0+
- LSPosed 1.10.2+ (Zygisk recommended)

### Steps

1. **Install Magisk** (if not already)
   - Download from [Magisk GitHub](https://github.com/topjohnwu/Magisk/releases)
   - Flash via recovery or direct install

2. **Install LSPosed**
   - Download Zygisk version from [LSPosed GitHub](https://github.com/LSPosed/LSPosed/releases)
   - Flash in Magisk as a module
   - Reboot device

3. **Install Device Masker**
   - Download APK from [Releases](https://github.com/your-repo/Device Masker/releases)
   - Install the APK
   - Open LSPosed Manager

4. **Enable Module**
   - In LSPosed Manager, find "Device Masker"
   - Enable the module
   - Select apps to hook (or leave empty for all)
   - **⚠️ IMPORTANT**: Do NOT select Device Masker itself!
   - Reboot device

5. **Configure Spoofing**
   - Open Device Masker app
   - Configure spoof values in "Spoof" tab
   - Select apps in "Apps" tab

---

## 🎯 Usage

### Quick Start

1. **Home Tab** - View module status and quick stats
2. **Apps Tab** - Select which apps to spoof
3. **Spoof Tab** - Configure spoof values by category
4. **Profiles Tab** - Create/manage spoof profiles
5. **Settings Tab** - App preferences and diagnostics

### Profiles

Profiles let you save different spoof configurations:

1. Open **Profiles** tab
2. Tap **+** to create new profile
3. Give it a name (e.g., "Banking Apps", "Games")
4. Values can be customized per profile
5. In **Apps** tab, assign profiles to specific apps

### Value Regeneration

- Tap the **refresh icon** on any value to regenerate
- Use "Regenerate All" in Home for all values
- Values persist across reboots

---

## ⚠️ Important Notes

### Do NOT Hook These
- **Device Masker itself** - Will crash the app
- **System apps** (generally) - May cause instability
- **LSPosed Manager** - Can break the framework

### Recommended Companion Modules

Device Masker focuses on device spoofing. For complete protection, use with:

| Protection | Module | Purpose |
|------------|--------|---------|
| Root Hiding | [Shamiko](https://github.com/LSPosed/LSPosed.github.io/releases) | Hide root from apps |
| SafetyNet | [Play Integrity Fix](https://github.com/chiteroman/PlayIntegrityFix) | Pass SafetyNet/Integrity |
| Bootloader | [Tricky Store](https://github.com/5ec1cff/TrickyStore) | Bypass keystore attestation |
| Zygisk | [Zygisk-Next](https://github.com/Dr-TSNG/ZygiskNext) | Zygisk implementation |

### Banking Apps
Most banking apps use multiple detection methods:
1. Root detection → Use Shamiko
2. SafetyNet/Play Integrity → Use Play Integrity Fix
3. Device fingerprinting → Use Device Masker
4. All three are typically needed together

---

## 🏗️ Build from Source

### Prerequisites
- Android Studio Ladybug (2024.1) or newer
- Java 21
- Kotlin 2.2.21

### Build Steps

```bash
# Clone repository
git clone https://github.com/your-repo/Device Masker.git
cd Device Masker

# Build debug APK
./gradlew assembleDebug

# Build release APK (requires signing config)
./gradlew assembleRelease
```

### Output
- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/app-release.apk`

---

## 🔧 Technical Details

### Architecture
```
Device Masker/
├── hook/                   # YukiHookAPI Layer
│   ├── HookEntry.kt        # Module entry point
│   └── hooker/             # Individual hookers
│       ├── AntiDetectHooker.kt
│       ├── DeviceHooker.kt
│       ├── NetworkHooker.kt
│       ├── AdvertisingHooker.kt
│       ├── SystemHooker.kt
│       └── LocationHooker.kt
├── data/                   # Data Layer
│   ├── SpoofDataStore.kt   # Preferences storage
│   ├── repository/         # Repository pattern
│   ├── models/             # Data classes
│   └── generators/         # Value generators
└── ui/                     # Jetpack Compose UI
    ├── screens/            # App screens
    ├── components/         # Reusable components
    ├── navigation/         # Navigation
    └── theme/              # Material 3 theming
```

### Technology Stack
| Component | Version |
|-----------|---------|
| Kotlin | 2.2.21 |
| YukiHookAPI | 1.3.1 |
| KavaRef | 1.0.2 |
| Compose BOM | 2025.12.00 |
| Material 3 | 1.4.0 |
| Target SDK | 36 (Android 16) |
| Min SDK | 26 (Android 8.0) |

### Hook Loading Order
1. **AntiDetectHooker** - Loaded FIRST for protection
2. **DeviceHooker** - Hardware identifiers
3. **NetworkHooker** - Network identifiers
4. **AdvertisingHooker** - Ad/tracking IDs
5. **SystemHooker** - Build properties
6. **LocationHooker** - GPS/timezone

---

## 🐛 Troubleshooting

### App Stuck at Logo
**Cause**: Module is hooking itself
**Solution**: 
1. Open LSPosed Manager
2. Find Device Masker in module scope
3. Remove `com.astrixforge.devicemasker` from selected apps
4. Reboot

### Module Not Working
1. Check LSPosed Manager shows module as activated
2. Verify target app is in module scope
3. Check LSPosed logs for errors
4. Force close target app and reopen

### Bootloop After Enable
1. Boot to recovery
2. Disable LSPosed module (delete from /data/adb/modules)
3. Or: Use Magisk safe mode (Vol Down during boot)

### Values Not Changing
1. Force close target app
2. Clear target app data (if safe)
3. Reboot device
4. Check if app is in module scope

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 🤝 Contributing

Contributions are welcome! Please:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing`)
5. Open a Pull Request

---

## ⚠️ Disclaimer

This module is for **educational and security research purposes only**. 

- Do NOT use to bypass security measures illegally
- Do NOT use for fraud or deception
- Users are responsible for compliance with local laws
- The developers are not responsible for misuse

---

## 📞 Support

- **Issues**: [GitHub Issues](https://github.com/your-repo/Device-Masker/issues)
- **Discussions**: [GitHub Discussions](https://github.com/your-repo/Device-Masker/discussions)

---

Made with ❤️ by AstrixForge
