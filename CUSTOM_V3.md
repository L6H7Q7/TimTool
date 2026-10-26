# TimTool 3.3 custom v3.3

This fork is based on TimTool 3.3 and adds a card-to-app action inspired by
[QAuxiliary](https://github.com/cinit/QAuxiliary). It is a custom build, not an
official TimTool release.

## Changes

- Adds **小程序与分享卡片跳转App** to the Ark card long-press menu. The target app
  packages, URL-matching expressions, and short-link handling can be configured
  from the feature's settings entry.
- The default Bilibili rule tries PiliPlus (`com.example.piliplus`) first, then
  the existing Bilibili packages, and Bilibili International (`com.bstar.intl`).
  It recognizes `b23.tv`, `bilibili.com`, `bilibili.tv`, and `bilibili.cn` links.
  An unchanged saved v3 default rule is upgraded automatically; custom rules
  are preserved. Installed apps must also accept the link in their manifest.
- Removes the **季节专属/雪花飘落** feature and its animation assets.
- On the first run of this custom v3, enables every feature shown in TimTool's
  settings, including the experimental category. Later manual switch choices
  are retained. **强制平板模式** may require signing in again.
- Removes module update checks, update dialogs, and the online update log entry.
  This custom version has no donation entry or donation calls.
- Removes the GitHub title, version text, and Telegram icon from the TIM settings
  dialog, leaving the feature categories and switches.
- Displays module version `3.3-custom-v3.3` (`versionCode` 37).

Build the module with `gradlew :app:assembleDebug` using the Android SDK required
by this project. The TIM 4.1.0 APK with an embedded module is distributed
separately; it is not committed to this source repository.
