package top.sacz.timtool.hook.item.chat

import top.sacz.timtool.hook.base.BaseSwitchFunctionHookItem
import top.sacz.timtool.hook.core.annotation.HookItem
import java.util.concurrent.CopyOnWriteArrayList

/** Remove TIM's reminder and to-do actions before the menu is laid out. */
@HookItem("辅助功能/聊天/移除提醒待办菜单")
class HideReminderTodoMenu : BaseSwitchFunctionHookItem() {
    override fun getTip() = "移除消息长按菜单中的提醒和待办选项"

    override fun loadHook(loader: ClassLoader) {
        val layoutClass = loader.loadClass("com.tencent.qqnt.aio.menu.ui.QQCustomMenuExpandableLayout")
        val menuClass = loader.loadClass("com.tencent.qqnt.aio.menu.ui.e")
        val itemsField = menuClass.getDeclaredField("d").apply { isAccessible = true }
        val titleMethod = loader.loadClass("com.tencent.qqnt.aio.menu.ui.f")
            .getDeclaredMethod("f").apply { isAccessible = true }
        val menuField = layoutClass.getDeclaredField("e").apply { isAccessible = true }

        hookBefore(layoutClass.getDeclaredMethod("show")) { param ->
            val menu = menuField.get(param.thisObject) ?: return@hookBefore
            val items = itemsField.get(menu) as? CopyOnWriteArrayList<*> ?: return@hookBefore
            items.removeIf { item ->
                val title = item?.let { titleMethod.invoke(it) as? String }?.trim()
                title in setOf("提醒", "待办", "设为待办", "加入待办", "添加待办", "设置提醒", "提醒待办")
            }
        }
    }
}
