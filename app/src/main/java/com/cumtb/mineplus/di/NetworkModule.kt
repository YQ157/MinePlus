package com.cumtb.mineplus.di

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import com.cumtb.mineplus.data.api.SchoolApi
import com.cumtb.mineplus.api.PersistentCookieStore
import com.cumtb.mineplus.api.SchoolApiConfig
import com.cumtb.mineplus.api.WebViewCookieJar
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class) // 安装在全局单例组件中
@SuppressLint("BadHostnameVerifier", "CustomX509TrustManager", "TrustAllX509TrustManager")
object NetworkModule {

    // 1. 提供我们写的那个 CookieJar
    @Provides
    @Singleton
    fun provideCookieJar(persistentCookieStore: PersistentCookieStore): WebViewCookieJar {
        return WebViewCookieJar(persistentCookieStore)
    }

    // 2. 组装 OkHttpClient (要把 CookieJar 装进去)
    @Provides
    @Singleton
    fun provideOkHttpClient(
        cookieJar: WebViewCookieJar,
        @ApplicationContext context: Context
    ): OkHttpClient {
        val isDebuggable = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0

        // 日志拦截器，方便你在 Logcat 里看请求和响应
        val logging = HttpLoggingInterceptor().apply {
            level = if (isDebuggable) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        val (sslSocketFactory, trustManager) = createTrustAllSslSocketFactory()

        return OkHttpClient.Builder()
            .cookieJar(cookieJar) // 👈 关键：装上 Cookie 立交桥
            .sslSocketFactory(sslSocketFactory, trustManager)
            .hostnameVerifier { host, _ -> SchoolApiConfig.isTrustedSchoolHost(host) }
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .callTimeout(45, TimeUnit.SECONDS)
            .build()
    }

    // 3. 组装 Retrofit
    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            // 👇 换成你们学校真实的教务系统域名
            .baseUrl(SchoolApiConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create()) // 支持 JSON 解析
            .build()
    }

    // 4. 生成可用的 Api 实例
    @Provides
    @Singleton
    fun provideSchoolApi(retrofit: Retrofit): SchoolApi {
        return retrofit.create(SchoolApi::class.java)
    }

    private fun createTrustAllSslSocketFactory(): Pair<SSLSocketFactory, X509TrustManager> {
        val trustManager = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit

            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit

            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        }

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf(trustManager), SecureRandom())

        return sslContext.socketFactory to trustManager
    }
}
