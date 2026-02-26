package com.cumtb.mineplus.api

import android.webkit.CookieManager
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

/**
 * 去 Android 系统的 WebView CookieManager 里拿。
 * WebView 登录成功后，Retrofit 自动就能拿到 Session，无需手动同步。
 */
class WebViewCookieJar : CookieJar {

    private val cookieManager = CookieManager.getInstance()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        // 当 Retrofit 请求回来带了新 Cookie 时，把它同步给 WebView
        val urlString = url.toString()
        cookies.forEach { cookie ->
            cookieManager.setCookie(urlString, cookie.toString())
        }
        cookieManager.flush()
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        // 当 Retrofit 发请求时，从 WebView 里取 Cookie 带上
        val urlString = url.toString()
        val cookieHeader = cookieManager.getCookie(urlString) ?: return emptyList()

        // 把 "key=value; key2=value2" 这种字符串切开，解析成 List<Cookie>
        val cookies = mutableListOf<Cookie>()
        cookieHeader.split(";").forEach { pair ->
            Cookie.parse(url, pair.trim())?.let { cookies.add(it) }
        }
        return cookies
    }
}