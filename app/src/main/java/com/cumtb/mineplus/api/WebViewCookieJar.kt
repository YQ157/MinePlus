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
class WebViewCookieJar(
    private val persistentCookieStore: PersistentCookieStore
) : CookieJar {

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
            persistentCookieStore.save(cookies)
            // 立即刷新到磁盘确保持久化
            cookieManager.flush()
            Log.d(tag, "✅ ${cookies.size} 个 Cookie 已保存并刷新到磁盘")
        } catch (e: Exception) {
            Log.e(tag, "❌ 保存 Cookie 失败", e)
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        return try {
            val urlString = url.toString()
            val cookieHeader = cookieManager.getCookie(urlString)

            if (!cookieHeader.isNullOrBlank()) {
                persistentCookieStore.saveFromHeader(url, cookieHeader)
                val webViewCookies = parseCookieHeader(url, cookieHeader)
                Log.d(tag, "✅ 从 WebView 加载了 ${webViewCookies.size} 个 Cookie")
                return webViewCookies
            }

            val persistedCookies = persistentCookieStore.loadForRequest(url)
            if (persistedCookies.isEmpty()) {
                Log.v(tag, "📭 $urlString 没有找到 Cookie")
                return emptyList()
            }

            persistedCookies.forEach { cookie ->
                cookieManager.setCookie(urlString, cookie.toString())
                Log.v(tag, "🍪 从本地缓存恢复 Cookie: ${cookie.name}")
            }
            cookieManager.flush()

            Log.d(tag, "✅ 从本地缓存加载了 ${persistedCookies.size} 个 Cookie")
            persistedCookies
        } catch (e: Exception) {
            Log.e(tag, "❌ 加载 Cookie 失败: ${url.host}", e)
            emptyList()
        }
    }

    private fun parseCookieHeader(url: HttpUrl, cookieHeader: String): List<Cookie> {
        return cookieHeader.split(";")
            .mapNotNull { pair -> Cookie.parse(url, pair.trim()) }
    }
}
