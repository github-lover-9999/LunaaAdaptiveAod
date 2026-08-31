<p align="center">
  <img src="assets/readme/hero-banner-v2.svg" alt="Lunaa Adaptive AOD Hero Banner" width="100%" />
</p>

<p align="center">
  <a href="https://github.com/github-lover-9999/LunaaAdaptiveAod/releases/latest"><img src="https://img.shields.io/badge/Release-v1.6.6-FFC449.svg?style=for-the-badge&logo=github" alt="Latest Release" /></a>
  <a href="https://github.com/github-lover-9999/LunaaAdaptiveAod/actions/workflows/ci.yml"><img src="https://img.shields.io/github/actions/workflow/status/github-lover-9999/LunaaAdaptiveAod/ci.yml?branch=main&style=for-the-badge&logo=githubactions&label=CI%20Build" alt="CI Status" /></a>
  <a href="https://github.com/github-lover-9999/LunaaAdaptiveAod/releases"><img src="https://img.shields.io/badge/Android-8.0_--_16_(API_36)-34D399.svg?style=for-the-badge&logo=android" alt="Android Support" /></a>
  <a href="https://github.com/mywalkb/LSPosed_mod"><img src="https://img.shields.io/badge/Xposed-LSPosed_/_Zygisk--Vector-38BDF8.svg?style=for-the-badge&logo=xposed" alt="LSPosed Support" /></a>
  <a href="https://kernelsu.org/"><img src="https://img.shields.io/badge/Root-KernelSU_/_Magisk_/_APatch-A78BFA.svg?style=for-the-badge&logo=superuser" alt="Root Support" /></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-GPL--3.0-F472B6.svg?style=for-the-badge" alt="License GPLv3" /></a>
</p>

<p align="center">
  <a href="https://github.com/github-lover-9999/LunaaAdaptiveAod/releases/download/v1.6.6/LunaaAdaptiveAod-v1.6.6-signed.apk"><b>📥 Download Latest APK (v1.6.6)</b></a> •
  <a href="#-requirements"><b>📋 Requirements</b></a> •
  <a href="#-how-to-install--use"><b>🚀 How to Install &amp; Use</b></a>
  <br>
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
   - Grab the latest signed APK from [Releases](https://github.com/github-lover-9999/LunaaAdaptiveAod/releases/latest) (or directly download [`LunaaAdaptiveAod-v1.6.6-signed.apk`](https://github.com/github-lover-9999/LunaaAdaptiveAod/releases/download/v1.6.6/LunaaAdaptiveAod-v1.6.6-signed.apk)).
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
  <img src="assets/readme/presets-breakdown.svg" alt="Lunaa Adaptive AOD Presets & Extra Bright Mode" width="100%" />
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
1. **Automatic Mode**: When using the **☀️ Bright** preset, Extra Bright automatically engages when the ambient sensor detects outdoor sunlight (**> 1,500 lux**). To prevent display driver desynchronization and ensure 100% reliable optical fingerprint unlocking, HBM remains safely latched for the duration of the AOD session until the screen is unlocked.
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

### 📖 О проекте
**Lunaa Adaptive AOD** — специализированный модуль для LSPosed с поддержкой Root, предназначенный для динамического, плавного и комфортного управления яркостью экрана Always-On Display (AOD) на смартфонах с AMOLED-матрицами. Разработан и полностью оптимизирован для **Realme GT Master Edition (RMX3363 / lunaa)** и кастомных прошивок на базе AOSP (crDroid, Axion OS, LineageOS и др.).

---

### 📋 Системные требования

| Компонент | Минимальные требования | Рекомендуемая конфигурация |
| :--- | :--- | :--- |
| **ОС Android** | Android 8.0 (Oreo / API 26) | Android 12 – 16 (API 31–36, AOSP / crDroid / Axion OS) |
| **Xposed фреймворк** | LSPosed / Zygisk-Vector | LSPosed (Zygisk релиз) с включенным скоупом `SystemUI` |
| **Root-доступ** | KernelSU, Magisk или APatch | KernelSU 3.x или Magisk (нужен для HBM и автообновлений) |
| **Дисплей** | AMOLED экран | **Realme GT Master Edition** (`RMX3363` / `lunaa`, Snapdragon 778G, Samsung AMOLED `AMS643YE01`) |

> [!NOTE]
> Математическая адаптивная кривая яркости работает на любых смартфонах с AMOLED экранами. Аппаратный режим HBM и защита оптического сканера отпечатка пальца используют драйверы Oplus (`/sys/kernel/oplus_display/notify_fppress`) с автоматической безопасной заглушкой для стандартных устройств AOSP.

---

### 🚀 Пошаговая установка и использование

1. **📥 Установка приложения**:
   - Скачайте свежий подписанный APK из раздела [Releases](https://github.com/github-lover-9999/LunaaAdaptiveAod/releases/latest) (или напрямую [`LunaaAdaptiveAod-v1.6.6-signed.apk`](https://github.com/github-lover-9999/LunaaAdaptiveAod/releases/download/v1.6.6/LunaaAdaptiveAod-v1.6.6-signed.apk)).
2. **⚙️ Активация в LSPosed**:
   - Откройте приложение **LSPosed Manager** (или Zygisk-Vector).
   - Включите модуль **Lunaa Adaptive AOD**.
   - Убедитесь, что в списке приложений для модуля отмечен **Системный интерфейс (SystemUI / `com.android.systemui`)**.
   - Перезагрузите смартфон для применения внедрения хуков.
3. **🎛️ Настройка и сохранение**:
   - Запустите **Lunaa Adaptive AOD** с рабочего стола.
   - Предоставьте **Root-права** при появлении системного запроса (необходимы для Extra Bright HBM и автообновления).
   - Выберите желаемый профиль (**⚡ Balanced** для повседневного использования или **☀️ Bright** для яркого солнца).
   - Нажмите **Save changes** (Сохранить). Заблокируйте телефон — адаптивный Always-On Display готов к работе!

---

### 🌟 Ключевые возможности

- 🌓 **Плавная адаптивная яркость**: Экран AOD в реальном времени подстраивается под данные датчика света по психофизическому закону Стивенса. Он не слепит глаза в темноте и отлично читается при обычном комнатном освещении.
- 🔥 **Аппаратный Extra Bright (AOD-HBM)**: Принудительный аппаратный перевод AMOLED-панели в пиковую яркость (~800 нит) на открытом солнце (>1500 люкс) или вручную.
- 🔓 **Полная совместимость с оптическим сканером (UDFPS)**: Фоновый логический сброс `/sys/kernel/oplus_display/notify_fppress` исключает зависание оптического сенсора — разблокировка пальцем остается мгновенной.
- 🛡️ **Защита для других прошивок (Axion OS / AOSP)**: Аппаратный зонд ядра проверяет наличие интерфейсов и безопасно отключает вызовы драйвера на сторонних устройствах.
- 🔄 **Встроенное автообновление с GitHub**: Проверка свежих версий прямо в приложении и 1-click тихая установка через Root (KernelSU / Magisk) либо стандартный установщик пакетов.

---

### 🎯 Режимы работы и пресеты яркости

1. **Автоматический режим (3 калиброванных пресета)**:
   - **🌙 DIM (Ночной режим)**: `20% – 40%` (~15–35 нит) — мягкий, не отвлекающий ночной циферблат для темных комнат и спальни.
   - **⚡ BALANCED (Повседневный, рекомендуемый)**: `50% – 76%` (~45–135 нит, масштабируется до 100% при 20 000 люкс) — комфортный базовый уровень 50% в темноте с плавным повышением в помещении.
   - **☀️ BRIGHT (Дневной режим)**: `100%` (215–380 нит) — максимальная яркость стандартной кривой AOD с авто-триггером Extra Bright на солнце.
2. **Ручной режим (Manual Mode)**:
   - 3 фиксированных положения ползунка: **Dim** (10%), **Balanced** (50%) и **Bright** (100%), точные проценты которых можно настроить в разделе Advanced Settings.

---

### 🔥 Аппаратный Extra Bright (AOD-HBM)

**Extra Bright** — режим пиковой аппаратной яркости High Brightness Mode, разработанный специально для AMOLED-экранов для обеспечения 100% читаемости Always-On Display под прямыми солнечными лучами.

#### Как это работает:
- Модуль напрямую взаимодействует с драйвером дисплея в ядре (`/sys/kernel/oplus_display/notify_fppress`) через защищенный Root-мост.
- Обходит стандартные ограничения яркости AOSP Doze и переводит AMOLED-матрицу в аппаратный режим HBM (**~800 нит**).

#### Условия активации:
1. **В авторежиме**: При выбранном пресете **☀️ Bright** Extra Bright автоматически включается, когда датчик света фиксирует уличное солнце (**> 1500 люкс**). Чтобы исключить сбои в работе оптического сканера и гарантировать 100% стабильную разблокировку пальцем, режим HBM надежно фиксируется до конца текущей сессии AOD (до разблокировки экрана).
2. **В ручном режиме**: При выборе 3 уровня яркости (Bright) включение тумблера **Extra Bright** активирует HBM на весь сеанс AOD.

#### Уровни мощности:
Ползунок Extra Bright поддерживает 3 калиброванных уровня силы:
- **Low**: `50%` мощности HBM.
- **Medium**: `75%` мощности HBM.
- **Max**: `100%` полная пиковая яркость панели (~800 нит).

#### 🔓 Мгновенная разблокировка по отпечатку (UDFPS):
На многих кастомных прошивках принудительное включение HBM блокирует работу оптического подэкранного сканера отпечатков. Lunaa Adaptive AOD решает эту проблему: root-демон мгновенно отправляет логический сброс (`notify_fppress = 0`), сохраняя физическую яркость на матрице. Оптический сканер не зависает и моментально распознает палец.

---

### 🛠️ Архитектура и внутренняя работа

1. **Внедрение хуков в SystemUI (`SystemUiHooks.java`)**:
   - Перехват состояний засыпания экрана `DozeScreenBrightness` и `transitionTo`.
   - Динамическое разрешение полей через `RuntimeFieldResolver` (`mSensorManager`, `mDisplayManager`, `mHandler`).
   - Мост `DozeBridge.java` для прозрачной поддержки сигнатур как `float` (0.0–1.0, modern AOSP), так и `int` (0–255, вендорные прошивки).
2. **Root-мост для ядра Oplus (`RootHbmBridgeReceiver.java`)**:
   - Защищенный прием команд строго от UID SystemUI.
   - Аппаратная фиксация HBM и автоматический сброс логического узла сканера отпечатка пальца.

---

## 📄 License & Credits

- Developed for the **Realme GT Master Edition** community.
- Licensed under the [GNU General Public License v3.0](LICENSE).