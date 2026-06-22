package com.cumtb.mineplus.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Build
import android.util.Log
import android.webkit.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SmartLoginWebView(
    username: String,
    password: String,
    onLoginSuccess: () -> Unit,
    onLoginFailed: (String) -> Unit,
    autoSubmitEnabled: Boolean = true,
    visible: Boolean = false,
    autoLoginTimeoutMs: Long = 12_000L,
) {
    val latestOnLoginSuccess = rememberUpdatedState(onLoginSuccess)
    val latestOnLoginFailed = rememberUpdatedState(onLoginFailed)

    // 仅用于避免“超时失败”重复通知；超时后仍应该继续监听页面，允许用户手动完成登录。
    val timeoutNotified = remember { mutableStateOf(false) }
    val lastFinishedUrl = remember { mutableStateOf<String?>(null) }

    // 自动模式下：如果超时仍停留在登录页，则通知上层“自动登录失败/超时”，但不阻断后续 success 检测。
    LaunchedEffect(autoSubmitEnabled, username, password) {
        if (!autoSubmitEnabled) return@LaunchedEffect
        timeoutNotified.value = false
        delay(autoLoginTimeoutMs)
        if (timeoutNotified.value) return@LaunchedEffect

        val url = lastFinishedUrl.value.orEmpty()
        val stillOnLogin = url.contains("authserver") || url.contains("login")
        if (stillOnLogin) {
            timeoutNotified.value = true
            latestOnLoginFailed.value("自动登录超时，请手动完成登录")
        }
    }

    AndroidView(
        // 隐藏态在 Compose 布局中不占空间；是否“覆盖全屏”交给上层决定（建议用 overlay Box）。
        modifier = if (visible) Modifier.fillMaxSize() else Modifier,
        factory = { context ->
            WebView(context).apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                }

                // 即使 AndroidView 自己不占空间，也让 WebView 本身有“最小尺寸”，避免 0 尺寸导致网页脚本异常。
                if (!visible) {
                    layoutParams = android.view.ViewGroup.LayoutParams(1, 1)
                    alpha = 0.01f
                }

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                }

                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        Log.e("SmartLogin", "🟡 开始加载: $url")
                    }

                    override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                        handler?.proceed() // 忽略 SSL 错误
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        val currentUrl = url ?: return
                        lastFinishedUrl.value = currentUrl
                        Log.e("SmartLogin", "🟢 加载完成: $currentUrl")

                        // 成功判断（自动/手动都适用）
                        if (currentUrl.contains("/student/home") || currentUrl.contains("index")) {
                            Log.e("SmartLogin", "🎉 登录成功")
                            latestOnLoginSuccess.value()
                            return
                        }

                        // 仅自动模式注入
                        if (!autoSubmitEnabled) return

                        val safePassword = password.replace("\\", "\\\\").replace("'", "\\'")

                        if (currentUrl.contains("login") || currentUrl.contains("authserver")) {
                            Log.e("SmartLogin", "💉 准备注入 JS (使用 evaluateJavascript)...")

                            val jsCode = """
                                (function() {
                                    console.log('MinePlus: JS Start');
                                    var attempts = 0;

                                    function tryFill() {
                                        attempts++;

                                        var u = document.querySelector("#nameInput") ||
                                                document.querySelector("#mobileUsername") ||
                                                document.querySelector("#username") ||
                                                document.querySelector("input[name='username']") ||
                                                document.querySelector("input[type='text']");

                                        var p = document.querySelector("input[type='password']");

                                        var btn = document.querySelector("#load") ||
                                                  document.querySelector("#submitBtn") ||
                                                  document.querySelector(".login-btn") ||
                                                  document.querySelector("button[type='submit']");

                                        if (u && p && btn) {
                                            console.log('MinePlus: Found Elements');
                                            u.value = '$username';
                                            u.dispatchEvent(new Event('input', {bubbles:true}));
                                            p.value = '$safePassword';
                                            p.type = 'text';
                                            p.dispatchEvent(new Event('input', {bubbles:true}));

                                            var remember = document.querySelector("#rememberMe");
                                            if(remember) remember.checked = true;

                                            setTimeout(function(){
                                                console.log('MinePlus: Clicking Login');
                                                btn.click();
                                            }, 500);
                                            return true;
                                        } else {
                                            console.log('MinePlus: Searching... ' + attempts);
                                            return false;
                                        }
                                    }

                                    var timer = setInterval(function() {
                                        if (tryFill() || attempts > 20) {
                                            clearInterval(timer);
                                        }
                                    }, 500);
                                })();
                            """.trimIndent()

                            view?.evaluateJavascript(jsCode, null)
                        }
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                        Log.d("SmartLoginJS", "${consoleMessage?.message()}")
                        return true
                    }
                }

                if (url == null) {
                    loadUrl("https://auth.cumtb.edu.cn/authserver/login?service=https%3A%2F%2Fjwxt.cumtb.edu.cn%2Fstudent%2Fsso%2Flogin")
                }
            }
        },
        update = { webView ->
            // 根据 visible 切换 WebView 自身“可操作/隐藏”形态
            if (visible) {
                webView.alpha = 1f
                webView.layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
            } else {
                webView.alpha = 0.01f
                webView.layoutParams = android.view.ViewGroup.LayoutParams(1, 1)
            }

            webView.settings.javaScriptCanOpenWindowsAutomatically = visible
            webView.settings.setSupportMultipleWindows(visible)

            if (webView.url == null) {
                webView.loadUrl("https://auth.cumtb.edu.cn/authserver/login?service=https%3A%2F%2Fjwxt.cumtb.edu.cn%2Fstudent%2Fsso%2Flogin")
            }
        }
    )
}
