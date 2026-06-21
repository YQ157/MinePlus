package com.cumtb.mineplus.di

import com.cumtb.mineplus.data.api.SchoolApi
import com.cumtb.mineplus.api.WebViewCookieJar
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class) // 安装在全局单例组件中
object NetworkModule {

    // 1. 提供我们写的那个 CookieJar
    @Provides
    @Singleton
    fun provideCookieJar(): WebViewCookieJar {
        return WebViewCookieJar()
    }

    // 2. 组装 OkHttpClient (要把 CookieJar 装进去)
    @Provides
    @Singleton
    fun provideOkHttpClient(cookieJar: WebViewCookieJar): OkHttpClient {
        // 日志拦截器，方便你在 Logcat 里看请求和响应
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .cookieJar(cookieJar) // 👈 关键：装上 Cookie 立交桥
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
            .baseUrl("https://jwxt.cumtb.edu.cn/")
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
}
