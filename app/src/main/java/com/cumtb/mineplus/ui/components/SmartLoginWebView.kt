package com.cumtb.mineplus.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.http.SslError
import android.util.Log
import android.webkit.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SmartLoginWebView(
    username: String,
    password: String,
    onLoginSuccess: () -> Unit,
    onLoginFailed: (String) -> Unit
) {
    // 修复问题2: 不要把 WebView 设为不可见或 size(0)，否则网页 JS 会崩。
    // 我们用 alpha(0.01f) 让它近乎透明，但实际上它占据了布局空间，网页能正常计算高度。
    AndroidView(
        modifier = Modifier.alpha(1f), // 👈 关键修改：隐身但不消失
        factory = { context ->
            WebView(context).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    // 伪装成电脑浏览器，有时候能规避移动端布局的 bug
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
                        Log.e("SmartLogin", "🟢 加载完成: $currentUrl")
                        Log.d("SmartLogin",password)
                        // 防止代码中的特殊符号破坏 JS
                        val safePassword = password.replace("\\", "\\\\").replace("'", "\\'")
                        Log.d("SmartLogin",safePassword)
                        // 注入逻辑
                        if (currentUrl.contains("login") || currentUrl.contains("authserver")) {
                            Log.e("SmartLogin", "💉 准备注入 JS (使用 evaluateJavascript)...")

                            // 修复问题1: 去掉所有 // 注释，使用纯净的代码，防止 SyntaxError
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

                            // 👈 关键修改：使用 evaluateJavascript 替代 loadUrl
                            view?.evaluateJavascript(jsCode, null)
                        }

                        // 成功判断
                        if (currentUrl.contains("/student/home") || currentUrl.contains("index")) {
                            Log.e("SmartLogin", "🎉 登录成功")
                            onLoginSuccess()
                        }
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                        Log.d("SmartLoginJS", "${consoleMessage?.message()}")
                        return true
                    }
                }
            }
        },
        update = { webView ->
            if (webView.url == null) {
                webView.loadUrl("https://auth.cumtb.edu.cn/authserver/login?service=https%3A%2F%2Fjwxt.cumtb.edu.cn%2Fstudent%2Fsso%2Flogin")
            }
        }
    )
}