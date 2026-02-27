package com.cumtb.mineplus.data.scraper

import android.util.Log
import org.jsoup.Jsoup
import java.util.regex.Pattern
object HtmlParser {
    /**
     * 从课表主页 HTML 中提取当前选中的学期 ID
     * 目标 HTML 结构:
     * <select id="allSemesters">
     * <option selected="selected" value="281">2024-2025-1</option>
     * </select>
     */
    fun parseSemesterId(html: String): Int {
        try {
            val doc = Jsoup.parse(html)

            // 1. 定位到下拉框里被选中的那个 option
            // 选择器语法: #id option[attribute]
            val selectedOption = doc.select("#allSemesters option[selected]").first()

            if (selectedOption != null) {
                val value = selectedOption.attr("value")
                Log.d("HtmlParser", "解析到 value: $value")

                // 2. 转成 Int 返回
                if (value.isNotEmpty() && value.all { it.isDigit() }) {
                    return value.toInt()
                }
            }

            // 如果没找到 selected，尝试找 value 最大的（通常是最新的学期）作为兜底
            // 这是一个容错策略，防止学校系统有时候不返回 selected 属性
            val allOptions = doc.select("#allSemesters option")
            if (allOptions.isNotEmpty()) {
                val maxId = allOptions.mapNotNull {
                    it.attr("value").toIntOrNull()
                }.maxOrNull()

                if (maxId != null) {
                    Log.w("HtmlParser", "未找到 selected 属性，降级使用最大 ID: $maxId")
                    return maxId
                }
            }

            throw Exception("无法从 HTML 中找到 semesterId")

        } catch (e: Exception) {
            Log.e("HtmlParser", "解析失败", e)
            // 如果实在解析失败，返回一个默认值（比如你之前看到的 281），防止 App 崩溃
            // 但最好还是抛出异常，让我们知道出错了
            throw e
        }
    }
    fun parseStdPersonId(html: String): Long {
        try {
            // 策略1：尝试匹配 JSON 格式的配置 (常见于新版强智)
            // 寻找 "stdPersonId":12345 或者是 "id":12345 (在 user 上下文中)
            // 这是一个比较通用的正则，匹配 "stdPersonId" 后面跟着冒号和数字
            val regex = Pattern.compile("data\\[['\"]stdPersonId['\"]\\]\\s*=\\s*(\\d+);")
            val matcher = regex.matcher(html)
            if (matcher.find()) {
                return matcher.group(1)?.toLong() ?: 0L
            }

            // 策略2：如果策略1失败，尝试匹配老式 JS 变量
            // if (html.contains("...")) ...

            // 如果实在找不到，可以在这里抛出异常，或者返回 -1 让 Repository 处理
            Log.e("HtmlParser", "⚠️ 未找到 stdPersonId，可能会导致 datum 请求失败")
            return -1L
        } catch (e: Exception) {
            e.printStackTrace()
            return -1L
        }
    }
}