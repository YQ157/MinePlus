package com.cumtb.mineplus

import android.app.Application
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebView
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MinePlusApp : Application() {
    
    companion object {
        private const val TAG = "MinePlusApp"
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // 初始化 WebView 和 Cookie 管理器
        initWebViewSettings()
    }
    
    private fun initWebViewSettings() {
        try {
            // 启用 WebView 的文件访问（调试模式下）
            WebView.setWebContentsDebuggingEnabled(true)
            
            // 初始化 Cookie 管理器
            val cookieManager = CookieManager.getInstance()
            
            // 启用 Cookie 接受
            cookieManager.setAcceptCookie(true)
            Log.d(TAG, "✅ Cookie 接受已启用")
            
            // 确保 Cookie 立即写入磁盘（增强持久化）
            cookieManager.flush()
            Log.d(TAG, "✅ WebView 设置初始化完成")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ WebView 设置初始化失败", e)
        }
    }
}
