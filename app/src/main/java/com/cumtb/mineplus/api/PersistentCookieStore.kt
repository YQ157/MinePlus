package com.cumtb.mineplus.api

import android.content.Context
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.Cookie
import okhttp3.HttpUrl
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PersistentCookieStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        FILE_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun save(cookies: List<Cookie>) {
        if (cookies.isEmpty()) return

        val now = System.currentTimeMillis()
        val storedCookies = readAll()
            .filter { it.expiresAt > now }
            .associateByTo(mutableMapOf()) { it.key }

        cookies.forEach { cookie ->
            val expiresAt = if (cookie.persistent) {
                cookie.expiresAt
            } else {
                now + SESSION_COOKIE_FALLBACK_TTL_MS
            }

            val storedCookie = StoredCookie(
                name = cookie.name,
                value = cookie.value,
                domain = cookie.domain,
                path = cookie.path,
                expiresAt = expiresAt,
                secure = cookie.secure,
                httpOnly = cookie.httpOnly,
                hostOnly = cookie.hostOnly
            )

            if (expiresAt <= now) {
                storedCookies.remove(storedCookie.key)
            } else {
                storedCookies[storedCookie.key] = storedCookie
            }
        }

        writeAll(storedCookies.values)
    }

    fun saveFromHeader(url: HttpUrl, cookieHeader: String): Boolean {
        val cookies = cookieHeader.split(";")
            .mapNotNull { part -> part.trim().toRequestCookie(url) }

        save(cookies)
        return cookies.isNotEmpty()
    }

    fun loadForRequest(url: HttpUrl): List<Cookie> {
        val now = System.currentTimeMillis()
        val allCookies = readAll()
        val validCookies = allCookies.filter { it.expiresAt > now }

        if (validCookies.size != allCookies.size) {
            writeAll(validCookies)
        }

        return validCookies
            .mapNotNull { it.toCookie() }
            .filter { it.matches(url) }
            .sortedByDescending { it.path.length }
            .distinctBy { "${it.domain}|${it.name}" }
    }

    fun hasCookiesFor(url: HttpUrl): Boolean {
        return loadForRequest(url).isNotEmpty()
    }

    fun hasCookiesForHost(host: String): Boolean {
        val now = System.currentTimeMillis()
        val allCookies = readAll()
        val validCookies = allCookies.filter { it.expiresAt > now }

        if (validCookies.size != allCookies.size) {
            writeAll(validCookies)
        }

        return validCookies.any { cookie ->
            if (cookie.hostOnly) {
                cookie.domain == host
            } else {
                host == cookie.domain || host.endsWith(".${cookie.domain}")
            }
        }
    }

    fun clear() {
        prefs.edit {
            remove(KEY_COOKIES)
        }
    }

    private fun readAll(): List<StoredCookie> {
        val raw = prefs.getString(KEY_COOKIES, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    array.optJSONObject(index)?.toStoredCookie()?.let(::add)
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun writeAll(cookies: Collection<StoredCookie>) {
        val array = JSONArray()
        cookies.forEach { cookie ->
            array.put(cookie.toJson())
        }

        prefs.edit {
            putString(KEY_COOKIES, array.toString())
        }
    }

    private data class StoredCookie(
        val name: String,
        val value: String,
        val domain: String,
        val path: String,
        val expiresAt: Long,
        val secure: Boolean,
        val httpOnly: Boolean,
        val hostOnly: Boolean
    ) {
        val key: String
            get() = "${if (hostOnly) "host" else "domain"}|$domain|$path|$name"

        fun toCookie(): Cookie? {
            val cookieDomain = domain
            val cookiePath = path
            return runCatching {
                Cookie.Builder()
                    .name(name)
                    .value(value)
                    .expiresAt(expiresAt)
                    .apply {
                        if (hostOnly) {
                            hostOnlyDomain(cookieDomain)
                        } else {
                            domain(cookieDomain)
                        }
                        path(cookiePath)
                        if (secure) secure()
                        if (httpOnly) httpOnly()
                    }
                    .build()
            }.getOrNull()
        }

        fun toJson(): JSONObject {
            return JSONObject()
                .put("name", name)
                .put("value", value)
                .put("domain", domain)
                .put("path", path)
                .put("expiresAt", expiresAt)
                .put("secure", secure)
                .put("httpOnly", httpOnly)
                .put("hostOnly", hostOnly)
        }
    }

    private fun JSONObject.toStoredCookie(): StoredCookie? {
        return runCatching {
            StoredCookie(
                name = getString("name"),
                value = getString("value"),
                domain = getString("domain"),
                path = getString("path"),
                expiresAt = getLong("expiresAt"),
                secure = optBoolean("secure", false),
                httpOnly = optBoolean("httpOnly", false),
                hostOnly = optBoolean("hostOnly", false)
            )
        }.getOrNull()
    }

    private fun String.toRequestCookie(url: HttpUrl): Cookie? {
        val separatorIndex = indexOf('=')
        if (separatorIndex <= 0) return null

        val name = substring(0, separatorIndex).trim()
        val value = substring(separatorIndex + 1).trim()
        if (name.isBlank()) return null

        return runCatching {
            Cookie.Builder()
                .name(name)
                .value(value)
                .hostOnlyDomain(url.host)
                .path(url.normalizedCookiePath())
                .expiresAt(System.currentTimeMillis() + SESSION_COOKIE_FALLBACK_TTL_MS)
                .apply {
                    if (url.isHttps) secure()
                    httpOnly()
                }
                .build()
        }.getOrNull()
    }

    private fun HttpUrl.normalizedCookiePath(): String {
        val firstSegment = pathSegments.firstOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: return "/"

        return "/$firstSegment"
    }

    private companion object {
        private const val FILE_NAME = "encrypted_cookies"
        private const val KEY_COOKIES = "cookies"
        private const val SESSION_COOKIE_FALLBACK_TTL_MS = 7L * 24L * 60L * 60L * 1000L
    }
}
