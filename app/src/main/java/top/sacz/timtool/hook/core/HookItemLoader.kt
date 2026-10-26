package top.sacz.timtool.hook.core

import top.sacz.timtool.hook.base.BaseSwitchFunctionHookItem
import top.sacz.timtool.hook.core.factory.HookItemFactory
import top.sacz.xphelper.util.ConfigUtils

class HookItemLoader {


    /**
     * 加载配置到对象
     */
    fun loadConfig() {
        val store = ConfigUtils("item_config")
        val visibleItems = HookItemFactory.getAllSwitchFunctionItemList()
            .filter { it.path.startsWith("辅助功能/") }

        // v3 首次运行时统一开启所有界面功能；之后尊重用户手动设置。
        if (!store.getBoolean("all_visible_enabled_v3", false)) {
            visibleItems.forEach { it.isEnabled = true }
            saveConfig()
            store.put("all_visible_enabled_v3", true)
            return
        }

        val config = store.getObject<Map<String, Boolean>>("item_config").orEmpty()
        visibleItems.forEach { item ->
            item.isEnabled = config[item.path] ?: true
        }
    }

    /**
     * 加载并判断哪些需要加载
     */
    fun loadHookItem() {
        //加载前进行一次api分配
        ApiProcessor.processor()
        //开始判断哪些需要加载
        val allHookItems = HookItemFactory.getAllItemList()
        allHookItems.forEach { hookItem ->
            if (hookItem is BaseSwitchFunctionHookItem && hookItem.isEnabled) {
                hookItem.startLoad()
            } else {
                hookItem.startLoad()
            }
        }
    }

    fun saveConfig() {
        val config = HashMap<String, Boolean>()
        val allHookItems = HookItemFactory.getAllSwitchFunctionItemList()
        allHookItems.forEach { hookItem ->
            config[hookItem.path] = hookItem.isEnabled
        }
        ConfigUtils("item_config").put("item_config", config)
    }
}
