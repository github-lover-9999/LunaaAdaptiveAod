<p align="center">
  <img src="assets/readme/hero-banner.svg" alt="Lunaa Adaptive AOD Hero Banner" width="100%" />
</p>

<p align="center">
  <a href="https://github.com/github-lover-9999/LunaaAdaptiveAod/releases/latest"><img src="https://img.shields.io/badge/Release-v1.6.5-FFC449.svg?style=for-the-badge&logo=github" alt="Latest Release" /></a>
  <a href="https://github.com/github-lover-9999/LunaaAdaptiveAod/releases"><img src="https://img.shields.io/badge/Android-8.0_--_16_(API_36)-34D399.svg?style=for-the-badge&logo=android" alt="Android Support" /></a>
  <a href="https://github.com/mywalkb/LSPosed_mod"><img src="https://img.shields.io/badge/Xposed-LSPosed_/_Zygisk--Vector-38BDF8.svg?style=for-the-badge&logo=xposed" alt="LSPosed Support" /></a>
  <a href="https://kernelsu.org/"><img src="https://img.shields.io/badge/Root-KernelSU_/_Magisk_/_APatch-A78BFA.svg?style=for-the-badge&logo=superuser" alt="Root Support" /></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-GPL--3.0-F472B6.svg?style=for-the-badge" alt="License GPLv3" /></a>
</p>

<p align="center">
  <a href="https://github.com/github-lover-9999/LunaaAdaptiveAod/releases/download/v1.6.5/LunaaAdaptiveAod-v1.6.5-signed.apk"><b>📥 Download Latest APK (v1.6.5)</b></a> •
  <a href="#-requirements"><b>📋 Requirements</b></a> •
  <a href="#-how-to-install--use"><b>🚀 How to Install &amp; Use</b></a> •
  <a href="#-presets--brightness-modes"><b>🎯 Presets &amp; Modes</b></a> •
  <a href="#-extra-bright-hardware-aod-hbm"><b>🔥 Extra Bright (HBM)</b></a> •
  <a href="#-architecture--how-it-works"><b>🛠️ Architecture</b></a> •
  <a href="#-русскоязычное-руководство"><b>🇷🇺 Русский гайд</b></a>
</p>

---

## 📋 Requirements

| Component | Minimum Requirement | Recommended / Tested Target |
| :--- | :--- | :--- |
| **Android OS** | Android 8.0 (Oreo / API 26) | Android 12 – 16 (API 31–36, AOSP / crDroid / Axion OS) |
| **Framework** | LSPosed / Zygisk-Vector | LSPosed (Zygisk release) with `SystemUI` scope enabled |
| **Root Access** | KernelSU, Magisk, or APatch | KernelSU 3.x or Magisk (required for Hardware HBM &amp; 1-click silent updates) |
| **Display Panel** | AMOLED Display | **Realme GT Master Edition** (`RMX3363` / `lunaa`, Snapdragon 778G, Samsung AMOLED `AMS643YE01`) |

> [!NOTE]
> The dynamic perceptual brightness curve works universally across all Android AMOLED devices. Hardware HBM panel latching and UDFPS touch rearming target Oplus display drivers (`/sys/kernel/oplus_display/notify_fppress`) with graceful safety fallback for generic AOSP hardware.

---

## 🚀 How to Install & Use

1. **📥 Download & Install APK**:
   - Grab the latest signed APK from [Releases](https://github.com/github-lover-9999/LunaaAdaptiveAod/releases/latest) (or directly download [`LunaaAdaptiveAod-v1.6.5-signed.apk`](https://github.com/github-lover-9999/LunaaAdaptiveAod/releases/download/v1.6.5/LunaaAdaptiveAod-v1.6.5-signed.apk)).
2. **⚙️ Enable in LSPosed**:
   - Open **LSPosed Manager** (or Zygisk-Vector).
   - Enable the **Lunaa Adaptive AOD** module.
   - Ensure **System Framework** / **SystemUI** (`com.android.systemui`) is checked in the module's scope.
   - Reboot your phone (recommended for initial LSPosed injection).
3. **🎛️ Configure & Save**:
   - Open **Lunaa Adaptive AOD** from your launcher.
   - Grant **Root access** when prompted (required for Extra Bright HBM and 1-click updates).
   - Select your preferred preset (**⚡ Balanced** for daily use or **☀️ Bright** for maximum outdoor visibility).
   - Tap **Save changes**. Lock your device to enjoy daylight-readable, adaptive Always-On Display!

---

## 🌟 Highlights & Key Features

- 🌓 **Truly Adaptive AOD Brightness**: Real-time ambient light sensor curve matching human perceptual brightness (Stevens' power law). Never too dim in room light, never blinding in a pitch-black room.
- 🔥 **Hardware AOD-HBM (Extra Bright)**: Direct panel hardware latching (~800 nits) under intense outdoor sunlight (>1500 lux) or on demand in manual mode.
- 🔓 **Full Optical UDFPS Compatibility**: Instant background logical rearm of `/sys/kernel/oplus_display/notify_fppress` so the optical in-display fingerprint sensor never gets blocked or frozen while HBM is active.
- 🛡️ **Axion OS & Multi-ROM Safety**: Hardware capability probe (`HbmCapabilityProbe`) with graceful fallback to standard AOSP ambient doze controls on non-Oplus firmware.
- 🔄 **In-App GitHub Auto-Updater**: Real-time release check directly from GitHub with 1-click seamless silent update via Root (KernelSU / Magisk) or standard Android package installer.

---

## 🎯 Presets & Brightness Modes

<p align="center">
  <img src="assets/readme/presets.svg" alt="Lunaa Adaptive AOD Presets & Extra Bright Mode" width="100%" />
</p>

### 1. Automatic Mode (3 Calibrated Presets)
- **🌙 DIM (Night Clock)**: `20% – 40%` (~15–35 nits) — soft, zero-glare, ideal for dark rooms and bedside tables.
- **⚡ BALANCED (Everyday Recommended)**: `50% – 76%` (~45–135 nits, up to 100% on 20,000 lux) — 50% comfortable floor in darkness scaling smoothly in indoor lighting.
- **☀️ BRIGHT (Daylight Mode)**: `100%` (215–380 nits) — maximum daytime visibility on standard AOD curve; automatically activates Extra Bright HBM outdoors.

### 2. Manual Mode (Fixed 3-Step Slider)
- Allows setting fixed brightness levels without ambient sensor adaptation:
  - **Dim**: Default 10% (customizable in Advanced Settings).
  - **Balanced**: Default 50% (customizable in Advanced Settings).
  - **Bright**: Default 100% (customizable in Advanced Settings, supports Extra Bright toggle).

---

## 🔥 Extra Bright (Hardware AOD-HBM)

**Extra Bright** is a specialized hardware-level High Brightness Mode engineered specifically for AMOLED screens to make the Always-On Display crystal clear under blinding direct sunlight.

### How It Works:
- Directly communicates with the kernel display driver (`/sys/kernel/oplus_display/notify_fppress`) via a secure root bridge.
- Bypasses standard AOSP doze brightness limits and forces the AMOLED panel into peak hardware HBM (**~800 nits**).

### How It Activates:
1. **Automatic Mode**: When using the **☀️ Bright** preset, Extra Bright automatically engages when the ambient sensor detects outdoor sunlight (**> 1,500 lux**), and turns off when moving indoors.
2. **Manual Mode**: When Manual Brightness is set to **Level 3 (Bright)**, enabling the **Extra Bright** toggle forces hardware HBM for the entire AOD session.

### Strength Levels:
The Extra Bright slider supports 3 calibrated strength levels (customizable under Advanced Settings):
- **Low**: `50%` HBM strength.
- **Medium**: `75%` HBM strength.
- **Max**: `100%` full panel HBM (~800 nits peak).

### 🔓 Optical Fingerprint (UDFPS) Instant Recovery:
On many custom ROMs, forcing HBM locks up the optical fingerprint scanner. Lunaa Adaptive AOD solves this by having the root daemon immediately perform a background logical reset (`notify_fppress = 0`) while preserving physical display latching. The optical fingerprint reader remains 100% responsive and unlocks instantly.

---

## 🛠️ Architecture & How It Works

<p align="center">
  <img src="assets/readme/architecture.svg" alt="Lunaa Adaptive AOD Architecture Flow" width="100%" />
</p>

### 1. SystemUI Hook Injection (`SystemUiHooks.java`)
Standard AOSP `DozeScreenBrightness` locks ambient brightness to a fixed, dim level (~4.8 nits / 0.016 float). Lunaa Adaptive AOD intercepts `transitionTo` and ambient state changes:
- Injects directly into `com.android.systemui` via LSPosed.
- Dynamically resolves runtime fields (`mSensorManager`, `mDisplayManager`, `mHandler`).
- Employs dual-type reflection bridge (`DozeBridge.java`) supporting both `float` (0.0–1.0, modern AOSP) and legacy `int` (0–255, custom vendor ROMs) `setDozeScreenBrightness` signatures.

### 2. Optical UDFPS & Hardware HBM Bridge (`RootHbmBridgeReceiver.java`)
On Snapdragon 778G / Samsung AMOLED (AMS643YE01) panels:
- Writing `1` to `/sys/kernel/oplus_display/notify_fppress` latches hardware High Brightness Mode (HBM).
- The root daemon securely accepts commands strictly from `com.android.systemui` and immediately executes a logical reset (`0`) in background.
- This keeps the physical HBM state locked in the display controller while restoring the optical sensor listener for immediate fingerprint unlocking.

---

## 🇷🇺 Русскоязычное руководство

### 📋 Требования:
- **Android**: Android 8.0 — Android 16 (AOSP, crDroid, Axion OS, LineageOS и др.).
- **Xposed**: LSPosed или Zygisk-Vector с выбранным скоупом `SystemUI`.
- **Root**: KernelSU, Magisk или APatch (нужен для HBM и 1-click автообновления).
- **Устройство**: Оптимизировано для Realme GT Master Edition (`RMX3363` / `lunaa`), базовая адаптивная яркость работает на любых AMOLED экранах.

### 🚀 Установка и настройка:
1. Скачайте и установите APK из раздела [Releases](https://github.com/github-lover-9999/LunaaAdaptiveAod/releases/latest).
2. В приложении **LSPosed Manager** включите модуль **Lunaa Adaptive AOD** и отметьте `SystemUI` в списке приложений.
3. Перезагрузите устройство для применения хука.
4. Откройте приложение, предоставьте Root-права при запросе, выберите профиль (**Balanced** или **Bright**) и нажмите **Save changes**.

### 🌟 Основные режимы работы:
1. **Автоматический режим**:
   - **🌙 DIM**: `20% – 40%` (~15–35 нит) — мягкий ночной циферблат без ослепления.
   - **⚡ BALANCED**: `50% – 76%` (~45–135 нит) — комфортный баланс для повседневного использования в помещении.
   - **☀️ BRIGHT**: `100%` (215–380 нит) — высокая базовая яркость + автоматическое включение Extra Bright на солнце.
2. **Ручной режим**: 3 фиксированных уровня яркости без привязки к датчику освещения.
3. **🔥 Аппаратный Extra Bright (AOD-HBM)**:
   - Включает пиковую аппаратную яркость дисплея (**~800 нит**) на открытом солнце (>1500 люкс) или вручную на 3 уровне.
   - Имеет 3 уровня силы: **Low (50%)**, **Medium (75%)**, **Max (100%)**.
   - **Разблокировка по отпечатку (UDFPS)**: При активном HBM сканер не залипает и мгновенно распознает палец.
4. **Встроенное автообновление с GitHub**: Проверка обновлений и установка в 1 клик через Root прямо из приложения.

---

## 📄 License & Credits

- Developed for the **Realme GT Master Edition** community.
- Licensed under the [GNU General Public License v3.0](LICENSE).