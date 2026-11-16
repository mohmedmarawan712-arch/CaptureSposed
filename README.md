# CaptureSposed
<img align="left" src="images/ic_launcher-playstore.png" width="115" />

With the release of Android 14, Google added an API to enable app developers to detect screenshots. This API has since been adopted by popular apps such as Snapchat.

CaptureSposed is an Xposed module that:
1. Effectively disables Android's official Screenshot Detection API (Android 14+) and Screen Recording Detection API (Android 15+).
2. Bypasses secure window restrictions (`FLAG_SECURE`) system-wide, allowing screenshots and screen recording anywhere, even in apps that forbid it (banking apps, streaming apps, protected chats, etc.).
3. Provides in-app switches and Quick Settings tiles to independently toggle each hook at runtime.

**⚠️ WARNING:** CaptureSposed is intended for rooted devices running Android 14 or newer and requires Xposed (LSPosed). Use at your own risk.

### Features
- **Block Screenshot Detection:** Prevents apps from knowing when a screenshot is taken.
- **Block Screen Recording Detection:** Prevents apps from knowing when screen recording is in progress.
- **Bypass Secure Windows (`FLAG_SECURE`):** Forces system-wide permission for capturing screenshots and screen recordings of secure windows and surfaces.
- **Quick Settings Tiles:** Dedicated toggles for each module feature directly from the notification shade.

### To use CaptureSposed:
1. Install an LSPosed build with API 101 support. This requires your device to be rooted with Magisk, KernelSU, or APatch.
2. Install CaptureSposed.
3. Grant root access to CaptureSposed.
4. Activate the CaptureSposed module in LSPosed.
5. Reboot your device.
6. Open the CaptureSposed app and use the switches to enable or disable each hook.
7. Optionally, add the Quick Settings tiles to easily toggle hooks from anywhere.
