# Lunaa Adaptive AOD

<p align="center">
  <b>Dynamic, smooth, and daylight-readable Always-On Display for Realme GT Master Edition (RMX3363 / lunaa) and AMOLED Android devices.</b><br>
  <i>Powered by LSPosed / Xposed and KernelSU / Magisk root bridge with optical In-Display Fingerprint (UDFPS) and Hardware AOD-HBM support.</i>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Android-8.0_--_16_(API_36)-green.svg" alt="Android Support" />
  <img src="https://img.shields.io/badge/Xposed-LSPosed_/_Zygisk--Vector-blue.svg" alt="LSPosed Support" />
  <img src="https://img.shields.io/badge/Root-KernelSU_/_Magisk_/_APatch-purple.svg" alt="Root Support" />
  <img src="https://img.shields.io/badge/License-GPLv3-orange.svg" alt="License" />
</p>

---

## 🌟 Highlights & Key Features

- 🌓 **Truly Adaptive AOD Brightness**: Real-time ambient light sensor curve matching human perceptual brightness (Stevens' power law).
- ☀️ **Hardware AOD-HBM (Extra Bright)**: Direct panel hardware latching (~800 nits) under intense outdoor sunlight (>1500 lux).
- 🔓 **Full Optical UDFPS Compatibility**: Instant logical rearm of `/sys/kernel/oplus_display/notify_fppress` so the optical fingerprint sensor never gets blocked or frozen while HBM is active.
- 🛡️ **Axion OS & Multi-ROM Safety**: Hardware capability probe (`HbmCapabilityProbe`) with graceful fallback to standard AOSP ambient doze controls on non-Oplus firmware.
- 🔄 **In-App GitHub Auto-Updater**: Real-time release check directly from GitHub with 1-click seamless silent update via Root (KernelSU) or standard Android package installer.
- 📱 **Three Calibrated Presets**:
  - **DIM (Night)**: `20% – 40%` (~15–35 nits) — soft, zero-glare night clock.
  - **BALANCED (Everyday)**: `50% – 76%` (~45–135 nits) — clearly visible in standard indoor light without blinding in the dark.
  - **BRIGHT (Daylight)**: `100%` (215–380 nits + HBM 800 nits) — maximum daytime clarity.

---

## 🛠️ Architecture & Under the Hood

### 1. SystemUI Hook Injection (`SystemUiHooks.java`)
Standard AOSP `DozeScreenBrightness` locks ambient brightness to a fixed, dim level (~4.8 nits / 0.016 float). Lunaa Adaptive AOD intercepts `transitionTo` and ambient state changes:
- Injects directly into `com.android.systemui` via LSPosed.
- Binds to `mSensorManager`, `mDisplayManager`, and `mHandler` via `RuntimeFieldResolver`.
- Employs dual-type reflection bridge (`DozeBridge.java`) to support both `float` (0.0–1.0, modern AOSP) and legacy `int` (0–255, custom vendor ROMs) `setDozeScreenBrightness` signatures.

### 2. Optical UDFPS & Hardware HBM Bridge (`RootHbmBridgeReceiver.java`)
On Snapdragon 778G / Samsung AMOLED (AMS643YE01) panels:
- Writing `1` to `/sys/kernel/oplus_display/notify_fppress` latches hardware High Brightness Mode (HBM).
- The root daemon securely accepts commands strictly from `com.android.systemui` and immediately executes a logical reset (`0`) in background.
- This keeps the physical HBM state locked in the display controller while restoring the optical sensor listener for immediate fingerprint unlocking.

---

## 📦 Installation & Setup

1. **Prerequisites**:
   - Android 8.0 through Android 16 (API 26–36).
   - Root via **KernelSU**, **Magisk**, or **APatch**.
   - **LSPosed** (or Zygisk-Vector) installed and activated.
2. **Install Lunaa Adaptive AOD**:
   - Download the latest signed APK from [Releases](https://github.com/github-lover-9999/LunaaAdaptiveAod/releases).
   - In LSPosed Manager, enable the **Lunaa Adaptive AOD** module and ensure **System Framework / SystemUI** is checked in its scope.
   - Reboot your device (recommended for initial LSPosed injection).
3. **Configure**:
   - Open **Lunaa Adaptive AOD** from your app drawer.
   - Grant Root access when prompted (for Extra Bright / Auto-Updater).
   - Choose your preferred Preset (**Balanced** or **Bright**) and tap **Save changes**.

---

## 🇷🇺 Русскоязычное руководство

### Описание
**Lunaa Adaptive AOD** — Xposed/LSPosed модуль с поддержкой Root для динамического управления яркостью Always-On Display на смартфонах Realme GT Master Edition (RMX3363 / lunaa) и других устройствах с AMOLED экранами.

### Основные возможности:
1. **Адаптивная яркость AOD**: Экран блокировки плавно подстраивается под окружающее освещение благодаря датчику света.
2. **Аппаратный Extra Bright (AOD-HBM)**: На ярком солнце дисплей переходит в режим пиковой яркости (~800 нит), обеспечивая отличную видимость циферблата и уведомлений.
3. **Работа оптического сканера отпечатка**: При включенном HBM сканер отпечатка не залипает и мгновенно разблокирует устройство.
4. **Безопасность для других прошивок (Axion OS / AOSP)**: Модуль проверяет совместимость ядра и отключает вендорные вызовы при отсутствии нужных интерфейсов Oplus.
5. **Встроенное автообновление с GitHub**: Проверка свежих релизов и установка обновлений прямо из настроек приложения.

---

## 📄 License & Credits

- Developed for the **Realme GT Master Edition** community.
- Licensed under the [GNU General Public License v3.0](LICENSE).