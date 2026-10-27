package top.sacz.timtool.hook.item.chat

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import top.sacz.timtool.hook.base.BaseSwitchFunctionHookItem
import top.sacz.timtool.hook.core.annotation.HookItem

/** TIM 4.1.0 only lays out two rows and reserves the last cell for an expand arrow. */
@HookItem("辅助功能/聊天/长按菜单显示全部项目")
class ShowAllMessageMenuItems : BaseSwitchFunctionHookItem() {
    override fun getTip() = "长按消息时直接显示全部可用菜单项，超过两行时继续向下排列"

    override fun loadHook(loader: ClassLoader) {
        val layoutClass = loader.loadClass("com.tencent.qqnt.aio.menu.ui.QQCustomMenuExpandableLayout")
        val menuField = layoutClass.getDeclaredField("e").apply { isAccessible = true }
        val rowField = layoutClass.getDeclaredField("p").apply { isAccessible = true }
        val columnsField = layoutClass.getDeclaredField("n").apply { isAccessible = true }
        val cellWidthField = layoutClass.getDeclaredField("i").apply { isAccessible = true }
        val rowHeightField = layoutClass.getDeclaredField("m").apply { isAccessible = true }
        val menuClass = loader.loadClass("com.tencent.qqnt.aio.menu.ui.e")
        val countMethod = menuClass.getDeclaredMethod("d").apply { isAccessible = true }
        val itemMethod = menuClass.getDeclaredMethod("b", Int::class.javaPrimitiveType).apply { isAccessible = true }
        val createItem = layoutClass.declaredMethods.first { method ->
            val p = method.parameterTypes
            method.returnType == View::class.java && p.size == 4 &&
                p[0] == Int::class.javaPrimitiveType && p[2] == Boolean::class.javaPrimitiveType &&
                p[3] == FloatArray::class.java
        }.apply { isAccessible = true }

        hookAfter(layoutClass.getDeclaredMethod("show")) { param ->
            val layout = param.thisObject as? RelativeLayout ?: return@hookAfter
            val menu = menuField.get(layout) ?: return@hookAfter
            val count = countMethod.invoke(menu) as Int
            val columns = columnsField.getInt(layout)
            if (columns <= 0 || count <= columns * 2) return@hookAfter
            val secondRow = rowField.get(layout) as? LinearLayout ?: return@hookAfter
            val arrow = secondRow.getChildAt(secondRow.childCount - 1)
            if (arrow !is ImageView) return@hookAfter

            val cellWidth = cellWidthField.getInt(layout)
            val rowHeight = rowHeightField.getInt(layout)
            if (cellWidth <= 0 || rowHeight <= 0) return@hookAfter

            secondRow.removeView(arrow)
            var row = secondRow
            for (index in columns * 2 - 1 until count) {
                if (index > columns * 2 - 1 && index % columns == 0) {
                    val previous = row
                    row = LinearLayout(layout.context).apply {
                        orientation = LinearLayout.HORIZONTAL
                        id = View.generateViewId()
                    }
                    val previousParams = previous.layoutParams as RelativeLayout.LayoutParams
                    val params = RelativeLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, rowHeight
                    ).apply {
                        addRule(RelativeLayout.BELOW, previous.id)
                        setMargins(previousParams.leftMargin, 0, previousParams.rightMargin, 0)
                    }
                    layout.addView(row, params)
                }
                val item = itemMethod.invoke(menu, index) ?: continue
                val view = createItem.invoke(layout, index, item, false, FloatArray(8)) as View
                if (row === secondRow) {
                    // The native second row uses RelativeLayout.LayoutParams for its cells.
                    secondRow.addView(view, RelativeLayout.LayoutParams(cellWidth, -1))
                } else {
                    val density = layout.resources.displayMetrics.density
                    view.setPadding(0, (8 * density).toInt(), 0, (16 * density).toInt())
                    row.addView(view, LinearLayout.LayoutParams(cellWidth, -1))
                }
            }
            layout.requestLayout()
        }
    }
}
