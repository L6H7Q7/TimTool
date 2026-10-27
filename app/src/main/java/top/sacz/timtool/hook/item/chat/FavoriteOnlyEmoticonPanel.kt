package top.sacz.timtool.hook.item.chat

import android.view.View
import top.sacz.timtool.hook.base.BaseSwitchFunctionHookItem
import top.sacz.timtool.hook.core.annotation.HookItem

/**
 * Use TIM's own favorite page and remove its tab strip in the chat emoticon panel.
 * The favorite page is loaded asynchronously on the first opening, so apply this
 * both when the panel is shown and after its initial tab query finishes.
 */
@HookItem("辅助功能/图片与表情/表情面板只显示收藏")
class FavoriteOnlyEmoticonPanel : BaseSwitchFunctionHookItem() {

    override fun getTip() = "点击聊天输入框的表情按钮时，直接显示收藏表情并隐藏底部切换栏"

    override fun loadHook(loader: ClassLoader) {
        val mainPanelClass = loader.loadClass("com.tencent.mobileqq.emoticonview.EmoticonMainPanel")
        val controllerClass = loader.loadClass("com.tencent.mobileqq.emoticonview.EmoticonPanelController")
        val basePanelViewClass = loader.loadClass("com.tencent.mobileqq.emoticonview.BasePanelView")

        val controllerField = mainPanelClass.getDeclaredField("mEmoController").apply { isAccessible = true }
        val mainPanelField = basePanelViewClass.getDeclaredField("mMainPanel").apply { isAccessible = true }
        val getBusinessType = controllerClass.getDeclaredMethod("getBusinessType")
        val findFavoriteIndex = controllerClass.getDeclaredMethod("findIndexByPanelType", Int::class.javaPrimitiveType)
        val getBasePanelView = controllerClass.getDeclaredMethod("getBasePanelView")
        val showPanelByTabIndex = basePanelViewClass.getDeclaredMethod(
            "showPanelByTabIndex", Int::class.javaPrimitiveType
        ).apply { isAccessible = true }
        val hideAllTabs = mainPanelClass.getDeclaredMethod("hideAllTabs")

        fun isChatPanel(panel: Any): Boolean {
            val controller = controllerField.get(panel) ?: return false
            // TIM uses business type 0 for its AIO chat input panel.
            return getBusinessType.invoke(controller) == 0
        }

        fun showFavorites(panel: Any) {
            if (!isChatPanel(panel)) return
            val controller = controllerField.get(panel) ?: return
            val index = findFavoriteIndex.invoke(controller, 4) as Int // PANEL_TYPE_FAVORITE
            if (index >= 0) {
                val baseView = getBasePanelView.invoke(controller) ?: return
                showPanelByTabIndex.invoke(baseView, index)
            }
            hideAllTabs.invoke(panel)
        }

        hookAfter(mainPanelClass.getDeclaredMethod("onShow")) { param ->
            showFavorites(param.thisObject)
        }
        hookAfter(basePanelViewClass.getDeclaredMethod(
            "initQueryFinish", Int::class.javaObjectType, Long::class.javaPrimitiveType
        )) { param ->
            val panel = mainPanelField.get(param.thisObject) ?: return@hookAfter
            showFavorites(panel)
        }
        hookAfter(basePanelViewClass.getDeclaredMethod("showAllTabs")) { param ->
            val panel = mainPanelField.get(param.thisObject) ?: return@hookAfter
            if (isChatPanel(panel) && (panel as View).visibility == View.VISIBLE) {
                hideAllTabs.invoke(panel)
            }
        }
    }
}
