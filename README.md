<h1 align="center">Essential Island</h1>

<p align="center">
  <strong>A Nothing OS-styled dynamic island for Android — notifications, media, calls and more in a monochrome pill around your camera cutout.</strong>
</p>

<p align="center">
  <a href="https://github.com/Jero009/essential-island/releases">
    <img src="https://img.shields.io/github/v/release/Jero009/essential-island?style=for-the-badge&color=FFC700&labelColor=000000&logo=github&logoColor=white" alt="Latest Release" />
  </a>
  <img src="https://img.shields.io/badge/Platform-Android%208.0%2B-3DDC84?style=for-the-badge&labelColor=000000&logo=android&logoColor=white" alt="Android 8.0+" />
  <img src="https://img.shields.io/badge/License-GNU%20GPLv3-3b82f6?style=for-the-badge&labelColor=000000&logo=gnu&logoColor=white" alt="GNU GPLv3" />
</p>

<p align="center">
  <a href="#what-it-is">What it is</a> &bull;
  <a href="#the-nothing-os-skin">The Nothing OS skin</a> &bull;
  <a href="#what-it-does">What it does</a> &bull;
  <a href="#privacy--permissions">Privacy</a> &bull;
  <a href="#building-from-source">Build</a> &bull;
  <a href="#credits">Credits</a>
</p>

---

## What it is

Essential Island puts a small black pill around your camera cutout or status bar. Notifications, music, calls, charging, timers and navigation show up in it, and a tap opens each one into a card. When nothing is going on, a clock, the weather, your net speed or the next alarm take turns in the same spot.

Everything happens on your device. There is no account, no analytics and no cloud.

---

## The Nothing OS skin

The interface follows the Nothing OS design language:

- **True-black OLED surfaces** — the pill is `#000000`, expanded cards `#1B1B1B`, with a white-ink text hierarchy (100% / 50% / 18%) and hairline borders.
- **Color is signal, not decoration** — red (`#D71921`) appears only for recording, live states, alarms and destructive actions. Everything else is monochrome.
- **Active states invert** — switches and selected controls turn white with black content, never colored.
- **Nothing typography** — dot-matrix numerals (Doto) for readouts, Geist / Geist Mono for UI text. All open-licensed (SIL OFL 1.1) and bundled with the app; no proprietary Nothing fonts are used.
- **Mechanical motion** — no bounce or spring overshoot; radii follow the 12 / 20 / 28 scale.
- **Choosable accent** — Settings › Appearance offers Nothing Yellow (`#FFC700`) and a small set of hues as the accent, applied only to progress and selected states, with dark-on-yellow content and danger always red.

Essential Island is **not affiliated with, endorsed by, or connected to Nothing Technology Limited.** "Nothing OS" styling is an independent interpretation of Nothing's public design language.

---

## What it does

### Notifications and live activities

Notifications show up as a small pill and open into a card with the app icon, the message and its action buttons. You can **reply inline**, and a half-typed reply is kept if a call or something else takes over the island. Alerts from the same app are **grouped into one card** with a small bubble to flip between them, and pictures in a notification open in steps from a preview to full size.

Other things it picks up and shows:

- **Music** with album art, a seek bar and track controls
- **Calls**, including **WhatsApp, Telegram and other VoIP calls**, with a call timer
- **Timers and stopwatches** from Google, Samsung, Xiaomi, Oppo and Huawei clock apps, with pause, resume and laps
- **Navigation** with the next turn, distance left and ETA
- **Rides and deliveries** from Uber, Swiggy, Zomato, Blinkit, Rapido and Ola
- **Downloads, uploads, backups, file copies and app installs** with live speed and time left
- **System events** like charging, low battery, battery saver, Wi-Fi, VPN, Bluetooth (with **earbud battery**), USB, headphones, hotspot, flashlight and screen or audio recording

There are about **30 events** and each has its own switch. Portrait and landscape can be set separately.

### Idle widgets

When nothing is going on, the cutout rotates through small widgets you pick: **clock, date, weather, wind, battery, next alarm, net speed, unread count and Do Not Disturb**. Tapping one opens more detail. The date widget shows your **next calendar event**, and the weather one can list rain chance, humidity, UV, air quality, sunrise and sunset and the next few hours.

### Gestures

The collapsed pill takes **swipes in all four directions**, so you can skip a track, cycle notifications or clear them without opening it. On an expanded card you can tap to open the app, swipe up to dismiss, **hold and swipe up to clear everything**, or swipe down for a **floating window** or the notification shade. Every direction can be remapped or turned off, and sensitivity and haptic strength are adjustable. There is a gesture guide with a small playground to try them.

### Quick launch

A launcher window opens from the island with **pinned apps, recents and a utilities grid**. The grid has Wi-Fi, Bluetooth, hotspot, airplane mode, dark mode, flashlight, calculator, timer, stopwatch, screenshot, screen and voice recorders, camera and clear cache, plus **resizable app widgets**.

### Looks and layout

- **Size, position and corner radius** are set separately for portrait and landscape, with presets for a centre hole, corner hole, wide island or compact pill.
- A **top-docked notch mode** sits flush against the edge, and the companion bubble can go on the left or right.
- **Custom colours and opacity**, **custom fonts** (any TTF or OTF), or a mix of up to three fonts by weight.
- **Animation speed** presets, plus a **landscape lab** to preview how the island behaves when you rotate.
- A **settings search**, and **backup and restore** to a file.

### Notification control

- **Cooldown** for apps that send bursts of alerts, and background sync pings are ignored.
- **Do Not Disturb** and Android modes are respected.
- **Per-app rules** for showing, sound or hiding, and an **exempt list** that switches the island off in games or video.
- **Lock screen presets**: everything, messages only, calls only or off.
- **Privacy dots** for camera, mic, location and screen sharing, like Android's own, and a tap shows which app.
- A **local, searchable history** of past notifications that never leaves the phone.

### Staying alive

**Shizuku** can set up the awkward permissions in one tap. There are tips for **Xiaomi, Samsung, OnePlus, Oppo, Huawei, Vivo, Asus and Nubia** so the system does not kill the service, and a baseline profile helps keep animations smooth.

### Languages

Essential Island is translated into **Spanish, Japanese, Brazilian Portuguese, and Simplified Chinese** in addition to English.

---

## Privacy & permissions

Essential Island asks for what it needs to do its job and nothing more:

- **Overlay and notification access** to draw the island and read alerts, media and timers.
- **Foreground service and battery optimisation** so Android keeps it alive.
- **Usage access, Shizuku** (optional) for one-tap setup and the privacy indicator app inspector.
- **Calendar, location and Bluetooth** only for the widgets that use them: next event, weather, and earbud battery.
- **Internet** only for the update check you choose to run and the weather lookup. No notification content ever leaves the device.

More in [PRIVACY.md](PRIVACY.md) and [SECURITY.md](SECURITY.md).

---

## Building from source

You need the Android SDK (API 26 to 36), JDK 21 and the bundled Gradle wrapper (9.7.1).

```bash
git clone https://github.com/Jero009/essential-island.git
cd essential-island
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

On Windows use `gradlew.bat`. Tests and lint run in separate invocations: `./gradlew testDebugUnitTest` and `./gradlew lintDebug`.

It is written in Kotlin with Jetpack Compose and Material 3, uses Hilt for injection, and keeps settings in DataStore and history in SQLite.

---

## Credits

Essential Island is a fork of **[SmartIsland](https://github.com/agupta07505/SmartIsland)** v7.6.0 by **Animesh Gupta**, licensed under the **GNU GPL v3.0**. All outstanding feature work — the island engine, notifications, gestures, widgets — is the original project's; this fork adds the package rebrand and the Nothing OS-styled skin.

Bundled open fonts: **Doto** (Óliver Lalan), **Geist** and **Geist Mono** (Vercel / Basement Studio), **Lora**, **Inter Display** and **Poppins** — all under the SIL Open Font License 1.1 (see [THIRD_PARTY_FONTS.md](THIRD_PARTY_FONTS.md)).

---

## License

Essential Island is licensed under the [GNU General Public License v3.0](LICENSE).
Copyright (C) 2026 **Animesh Gupta** (original SmartIsland), with modifications Copyright (C) 2026 **Jero009**.

This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
