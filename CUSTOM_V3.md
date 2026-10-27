# TimTool 3.3 custom v3.7

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
- Adds **表情面板只显示收藏**: opening TIM's chat emoticon panel selects the
  native favorite page and hides its bottom tab strip. This is independent of
  TimTool's long-press custom sticker panel.
- Opens matching Ark share cards in a configured app on a short tap. The
  long-press **打开** menu item is removed; long presses and unmatched cards keep
  TIM's original behavior.
- Prevents duplicate **复读** long-press menu items when TIM or another menu
  callback already provides one.
- Shows every available long-press message menu item immediately, adding rows
  below TIM's native two-row menu when necessary.
- Removes TIM's **提醒** and **待办** long-press menu actions. The **复读** action
  remains controlled by its existing TimTool switch.
- Displays module version `3.3-custom-v3.7` (`versionCode` 41).

Build the module with `gradlew :app:assembleDebug` using the Android SDK required
by this project. The TIM 4.1.0 APK with an embedded module is distributed
separately; it is not committed to this source repository.
