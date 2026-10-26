package top.sacz.timtool.ui.dialog

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kongzue.dialogx.dialogs.MessageDialog
import com.kongzue.dialogx.interfaces.OnBindView
import com.kongzue.dialogx.util.FixContextUtil
import top.sacz.timtool.R
import top.sacz.timtool.ui.adapter.CategoryAdapter
import top.sacz.timtool.ui.adapter.SettingUIItemManger
import top.sacz.timtool.ui.bean.Category
import top.sacz.timtool.ui.bean.ParentCategory

class SettingDialog {
    private lateinit var dialog: MessageDialog

    private lateinit var rootView: ViewGroup

    private var isCategory = true


    @SuppressLint("InflateParams")
    fun show() {
        dialog = MessageDialog.build()
            .setCustomView(object : OnBindView<MessageDialog>(R.layout.layout_setting) {
                override fun onBind(
                    p0: MessageDialog,
                    p1: View
                ) {
                    rootView = p1 as ViewGroup
                    dialog = p0
                    onBindView()
                }
            })
            .show()
    }

    fun refresh() {
        onBindView()
    }

    @SuppressLint("InflateParams")
    private fun onBindView() {
        val context = rootView.context
        //提交初始的分类
        val boxView = rootView.findViewById<FrameLayout>(R.id.box_list)
        val categoryRv = FixContextUtil.getFixInflater(context)
            .inflate(R.layout.layout_setting_category_list, null, false) as RecyclerView
        val rvAdapter = CategoryAdapter(getInitCategory())
        rvAdapter.setOnItemClickListener { adapter, view, position ->
            val category = adapter.getItem(position)
            if (category is Category) {
                //隐藏分类view
                isCategory = false
                boxView.getChildAt(0).visibility = View.GONE
                val newItemViewRv = FixContextUtil.getFixInflater(context)
                    .inflate(R.layout.layout_setting_category_list, null, false) as RecyclerView
                val newItemAdapter = CategoryAdapter(category.items)
                if (newItemAdapter.getItem(0) !is ParentCategory) {
                    newItemAdapter.add(0, ParentCategory(category.title))
                }
                newItemViewRv.layoutManager = LinearLayoutManager(context)
                newItemViewRv.adapter = newItemAdapter
                boxView.addView(newItemViewRv)
            }
        }
        categoryRv.layoutManager = LinearLayoutManager(context)
        categoryRv.adapter = rvAdapter
        boxView.addView(categoryRv)
        //设置返回处理
        dialog.setOnBackPressedListener {
            onBack(boxView)
        }
        dialog.setOnBackgroundMaskClickListener { dialog, v ->
            !onBack(boxView)
        }
    }

    private fun onBack(boxView: ViewGroup): Boolean {
        if (isCategory) {
            return true
        } else {
            isCategory = true
            boxView.removeViewAt(boxView.childCount - 1)
            boxView.getChildAt(0).visibility = View.VISIBLE
            return false
        }
    }

    private fun getInitCategory(): List<Any> {
        val result = mutableListOf<Any>()
        val uiItemManger = SettingUIItemManger()
        for (parentCategory in uiItemManger.parseHookItemAsUI()) {
            result.add(parentCategory)
            result.addAll(parentCategory.categoryList)
        }
        return result
    }

}
