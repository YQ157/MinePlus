package com.cumtb.mineplus.api

import android.util.Log
import android.webkit.CookieManager
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

/**
 * 去 Android 系统的 WebView CookieManager 里拿。
 * WebView 登录成功后，Retrofit 自动就能拿到 Session，无需手动同步。
 * 
 * 改进版本：增强 Cookie 持久化和调试能力
 */
class WebViewCookieJar : CookieJar {

    private val cookieManager = CookieManager.getInstance()
    private val tag = "WebViewCookieJar"

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        try {
            // 当 Retrofit 请求回来带了新 Cookie 时，把它同步给 WebView
            val urlString = url.toString()
            cookies.forEach { cookie ->
                cookieManager.setCookie(urlString, cookie.toString())
                Log.v(tag, "💾 保存 Cookie: ${cookie.name}=${cookie.value} for $urlString")
            }
            // 立即刷新到磁盘确保持久化
            cookieManager.flush()
            Log.d(tag, "✅ ${cookies.size} 个 Cookie 已保存并刷新到磁盘")
        } catch (e: Exception) {
            Log.e(tag, "❌ 保存 Cookie 失败", e)
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        return try {
            // 当 Retrofit 发请求时，从 WebView 里取 Cookie 带上
            val urlString = url.toString()
            val cookieHeader = cookieManager.getCookie(urlString)
            
            if (cookieHeader.isNullOrBlank()) {
                Log.v(tag, "📭 $urlString 没有找到 Cookie")
                return emptyList()
            }

            // 把 "key=value; key2=value2" 这种字符串切开，解析成 List<Cookie>
            val cookies = mutableListOf<Cookie>()
            cookieHeader.split(";").forEach { pair ->
                Cookie.parse(url, pair.trim())?.let { cookie ->
                    cookies.add(cookie)
                    Log.v(tag, "🍪 加载 Cookie: ${cookie.name}=${cookie.value}")
                }
            }
            
            Log.d(tag, "✅ 从 $urlString 加载了 ${cookies.size} 个 Cookie")
            cookies
        } catch (e: Exception) {
            Log.e(tag, "❌ 加载 Cookie 失败: ${url.host}", e)
            emptyList()
        }
    }
}