package top.sacz.timtool.hook.item.chat

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import org.json.JSONArray
import org.json.JSONObject
import top.sacz.timtool.hook.base.BaseSwitchFunctionHookItem
import top.sacz.timtool.hook.core.annotation.HookItem
import top.sacz.timtool.hook.util.ToastTool
import top.sacz.timtool.hook.util.callMethod
import top.sacz.xphelper.util.ActivityTools
import top.sacz.xphelper.util.ConfigUtils
import java.net.HttpURLConnection
import java.net.URL
import java.util.WeakHashMap

/** Open a tapped Ark share card in a selected installed app. */
@HookItem("辅助功能/聊天/小程序与分享卡片跳转App")
class MiniAppDirectJump : BaseSwitchFunctionHookItem() {

    private data class AppRule(val packages: String, val pattern: String, val resolveShortUrl: Boolean)
    private data class TouchStart(val x: Float, val y: Float, val downTime: Long)

    private val config get() = ConfigUtils("MiniAppDirectJump")
    private val cardDataByView = WeakHashMap<View, String>()
    private val touchStarts = WeakHashMap<View, TouchStart>()

    private val oldBiliPackages =
        "tv.danmaku.bili,com.bilibili.app.in,tv.danmaku.bilibilihd,com.bilibili.app.blue"
    private val biliPackages =
        "com.example.piliplus,tv.danmaku.bili,com.bilibili.app.in,tv.danmaku.bilibilihd,com.bilibili.app.blue,com.bstar.intl"
    private val oldBiliPattern = "https?:\\\\?/\\\\?/((b23\\.tv)|([\\w.]*bilibili\\.com))[^\\\" ]*"
    private val biliPattern = "https?:\\\\?/\\\\?/((b23\\.tv)|([\\w.]*bilibili\\.(com|tv|cn)))[^\\\" ]*"

    private fun defaultRules() = listOf(
        AppRule(biliPackages, biliPattern, true),
        AppRule("com.xingin.xhs", "(?<=\\\"jumpUrl\\\":\\\")https?:\\\\?/\\\\?/[^\\\"]*", false),
        AppRule("com.coolapk.market", "(?<=\\\"jumpUrl\\\":\\\")https?:\\\\?/\\\\?/[^\\\"]*", false),
        AppRule("com.zhihu.android", "(?<=\\\"jumpUrl\\\":\\\")https?:\\\\?/\\\\?/[^\\\"]*", false)
    )

    private fun rules(): List<AppRule> {
        val stored = config.getString("rules", "")
        if (stored.isEmpty()) return defaultRules()
        val parsed = runCatching {
            val array = JSONArray(stored)
            (0 until array.length()).map { index ->
                val item = array.getJSONObject(index)
                AppRule(item.getString("packages"), item.getString("pattern"), item.optBoolean("resolveShortUrl"))
            }
        }.getOrDefault(defaultRules())
        // Upgrade an unchanged saved v3 default without overwriting custom rules.
        val upgraded = parsed.map { rule ->
            if (rule.packages == oldBiliPackages && rule.pattern == oldBiliPattern) {
                rule.copy(packages = biliPackages, pattern = biliPattern)
            } else rule
        }
        if (upgraded != parsed) saveRules(upgraded)
        return upgraded
    }

    private fun saveRules(rules: List<AppRule>) {
        val array = JSONArray()
        rules.forEach { rule ->
            array.put(JSONObject().put("packages", rule.packages).put("pattern", rule.pattern)
                .put("resolveShortUrl", rule.resolveShortUrl))
        }
        config.put("rules", array.toString())
    }

    override fun getTip() = "轻点分享卡片直接跳转 App；点击此项可设置包名与链接匹配规则"

    override fun getOnClickListener() = View.OnClickListener { view -> showSettings(view) }

    private fun showSettings(view: View) {
        val ctx = view.context
        val density = ctx.resources.displayMetrics.density
        val padding = (16 * density).toInt()
        val container = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
        }
        data class RuleEditor(val root: LinearLayout, val packages: EditText, val pattern: EditText, val shortUrl: CheckBox)
        val editors = mutableListOf<RuleEditor>()

        fun addEditor(rule: AppRule) {
            val row = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
            val packages = EditText(ctx).apply {
                hint = "应用包名，多个用英文逗号分隔"
                setSingleLine(true)
                setText(rule.packages)
            }
            val pattern = EditText(ctx).apply {
                hint = "从卡片 JSON 匹配链接的正则表达式"
                setText(rule.pattern)
            }
            val shortUrl = CheckBox(ctx).apply {
                text = "展开短链接"
                isChecked = rule.resolveShortUrl
            }
            val editor = RuleEditor(row, packages, pattern, shortUrl)
            val remove = Button(ctx).apply {
                text = "删除这条规则"
                setOnClickListener {
                    container.removeView(row)
                    editors.remove(editor)
                }
            }
            row.addView(packages)
            row.addView(pattern)
            row.addView(shortUrl)
            row.addView(remove)
            container.addView(row)
            editors.add(editor)
        }

        rules().forEach(::addEditor)
        val add = Button(ctx).apply {
            text = "添加规则"
            setOnClickListener { addEditor(AppRule("", "", false)) }
        }
        container.addView(add)
        val scroll = ScrollView(ctx).apply { addView(container) }
        AlertDialog.Builder(ctx).setTitle("卡片跳转 App")
            .setView(scroll)
            .setNegativeButton("取消", null)
            .setPositiveButton("保存") { _, _ ->
                val updated = editors.map {
                    AppRule(it.packages.text.toString().trim(), it.pattern.text.toString().trim(), it.shortUrl.isChecked)
                }.filter { it.packages.isNotEmpty() && it.pattern.isNotEmpty() }
                if (updated.any { runCatching { Regex(it.pattern) }.isFailure }) {
                    ToastTool.show("正则表达式无效，配置未保存")
                } else {
                    saveRules(updated)
                    ToastTool.show("跳转配置已保存")
                }
            }.show()
    }

    override fun loadHook(loader: ClassLoader) {
        val componentClass = loader.loadClass(
            "com.tencent.mobileqq.aio.msglist.holder.component.ark.AIOArkContentComponent"
        )
        val arkViewClass = loader.loadClass(
            "com.tencent.mobileqq.aio.msglist.holder.component.ark.view.ArkAppView"
        )
        val arkViewField = componentClass.getDeclaredField("H").apply { isAccessible = true }
        val bindCard = componentClass.declaredMethods.first {
            it.name == "C2" && it.parameterTypes.size == 4
        }
        hookAfter(bindCard) { param ->
            val cardView = arkViewField.get(param.thisObject) as? View ?: return@hookAfter
            val element = param.args[1]
            val data = runCatching {
                element.callMethod<Any>("getArkElement").callMethod<String>("getBytesData")
            }.getOrNull()
            cardDataByView.remove(cardView)
            if (!data.isNullOrBlank()) cardDataByView[cardView] = data
        }
        hookBefore(arkViewClass.getDeclaredMethod(
            "onTouch", View::class.java, MotionEvent::class.java
        )) { param ->
            val cardView = param.thisObject as View
            val event = param.args[1] as MotionEvent
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> touchStarts[cardView] = TouchStart(event.x, event.y, event.downTime)
                MotionEvent.ACTION_MOVE -> {
                    val start = touchStarts[cardView] ?: return@hookBefore
                    val slop = ViewConfiguration.get(cardView.context).scaledTouchSlop
                    if (kotlin.math.abs(event.x - start.x) > slop ||
                        kotlin.math.abs(event.y - start.y) > slop
                    ) touchStarts.remove(cardView)
                }
                MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_CANCEL -> touchStarts.remove(cardView)
                MotionEvent.ACTION_UP -> {
                    val start = touchStarts.remove(cardView) ?: return@hookBefore
                    val slop = ViewConfiguration.get(cardView.context).scaledTouchSlop
                    if (event.downTime != start.downTime ||
                        kotlin.math.abs(event.x - start.x) > slop ||
                        kotlin.math.abs(event.y - start.y) > slop ||
                        event.eventTime - start.downTime >= ViewConfiguration.getLongPressTimeout()
                    ) return@hookBefore
                    val data = cardDataByView[cardView] ?: return@hookBefore
                    val matches = matchingRules(data)
                    if (matches.isEmpty()) return@hookBefore
                    val activity = ActivityTools.getTopActivity() ?: return@hookBefore
                    openCard(activity, matches)
                    param.result = true
                }
            }
        }
    }

    private fun matchingRules(data: String): List<Pair<AppRule, String>> = rules().mapNotNull { rule ->
            val result = runCatching { Regex(rule.pattern).find(data) }.getOrNull() ?: return@mapNotNull null
            val url = result.value.replace("\\/", "/").replace("&amp;", "&")
            if (!url.startsWith("http://") && !url.startsWith("https://")) return@mapNotNull null
            rule to url
        }

    private fun openCard(activity: Activity, matched: List<Pair<AppRule, String>>) {
        Thread {
            val candidates = matched.map { (rule, url) ->
                val resolved = if (rule.resolveShortUrl && Uri.parse(url).host == "b23.tv") {
                    runCatching { resolveUrl(url) }.getOrDefault(url)
                } else url
                resolved to rule.packages.split(',').map { it.trim() }.filter { it.isNotEmpty() }
            }
            activity.runOnUiThread {
                if (candidates.none { (url, packages) -> launchApp(activity, url, packages) }) {
                    ToastTool.show("没有已安装的应用可以打开此链接")
                }
            }
        }.start()
    }

    private fun resolveUrl(url: String): String {
        var current = url
        repeat(3) {
            val connection = (URL(current).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                connectTimeout = 3000
                readTimeout = 3000
            }
            val next = try {
                if (connection.responseCode in 300..399) connection.getHeaderField("Location") else null
            } catch (_: Exception) {
                null
            } finally {
                connection.disconnect()
            }
            if (next.isNullOrBlank()) return current
            current = runCatching { URL(URL(current), next).toString() }.getOrDefault(current)
            if (!current.startsWith("https://") && !current.startsWith("http://")) return url
        }
        return current
    }

    private fun launchApp(activity: Activity, url: String, packages: List<String>): Boolean {
        for (packageName in packages) {
            try {
                activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply { setPackage(packageName) })
                return true
            } catch (_: ActivityNotFoundException) {
                // Try the next configured app.
            } catch (_: SecurityException) {
                // The app does not expose an activity for this URL.
            }
        }
        return false
    }
}
