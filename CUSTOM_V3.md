# TimTool 3.3 custom v3

This fork is based on TimTool 3.3 and adds a card-to-app action inspired by
[QAuxiliary](https://github.com/cinit/QAuxiliary). It is a custom build, not an
official TimTool release.

## Changes

- Adds **小程序与分享卡片跳转App** to the Ark card long-press menu. The target app
  packages, URL-matching expressions, and short-link handling can be configured
  from the feature's settings entry.
- Removes the **季节专属/雪花飘落** feature and its animation assets.
- On the first run of this custom v3, enables every feature shown in TimTool's
  settings, including the experimental category. Later manual switch choices
  are retained. **强制平板模式** may require signing in again.
- Displays module version `3.3-custom-v3` (`versionCode` 34).

Build the module with `gradlew :app:assembleDebug` using the Android SDK required
by this project. The TIM 4.1.0 APK with an embedded module is distributed
separately; it is not committed to this source repository.
