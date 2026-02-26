package com.cumtb.mineplus.ui.login

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.cumtb.mineplus.ui.MainViewModel
import com.cumtb.mineplus.ui.components.SmartLoginWebView
import com.cumtb.mineplus.ui.theme.Dimens
import kotlinx.coroutines.launch

private enum class WebLoginMode {
    Hidden,
    Auto,
    Manual
}

@Composable
fun LoginScreen(
    viewModel: MainViewModel = hiltViewModel(),
    onLoginSuccess: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rememberPassword by remember { mutableStateOf(false) }

    var webLoginMode by remember { mutableStateOf(WebLoginMode.Hidden) }
    val isLoggingIn = webLoginMode == WebLoginMode.Auto

    var loginStatusText by remember { mutableStateOf("准备就绪") }

    // 首次进入时回填
    LaunchedEffect(Unit) {
        val state = viewModel.loadSavedCredentials()
        rememberPassword = state.rememberPassword
        if (state.rememberPassword) {
            username = state.username
            password = state.password
        }
    }

    // 用 Box 做“表单 + 覆盖层 WebView”，确保手动 WebView 显示时不挤占 Column 的布局空间。
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(Dimens.screenPadding)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "Mine+", style = MaterialTheme.typography.headlineLarge)

            Spacer(modifier = Modifier.height(Dimens.medium2))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text(text = "学号", style = MaterialTheme.typography.labelLarge) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(Dimens.elementSpacing))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(text = "密码", style = MaterialTheme.typography.labelLarge) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    autoCorrect = false
                ),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(Dimens.small))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Switch(
                    checked = rememberPassword,
                    onCheckedChange = { rememberPassword = it }
                )
                Spacer(modifier = Modifier.width(Dimens.tiny2))
                Text(text = "记住密码", style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(modifier = Modifier.height(Dimens.elementSpacing))

            Button(
                onClick = {
                    webLoginMode = WebLoginMode.Auto
                    loginStatusText = "正在尝试自动登录..."
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimens.buttonHeight),
                enabled = webLoginMode == WebLoginMode.Hidden && username.isNotBlank() && password.isNotBlank()
            ) {
                if (isLoggingIn) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(Dimens.small2),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(Dimens.tiny2))
                    Text(text = "登录中...", style = MaterialTheme.typography.labelLarge)
                } else {
                    Text(text = "登录", style = MaterialTheme.typography.labelLarge)
                }
            }

            Spacer(modifier = Modifier.height(Dimens.elementSpacing))

            Text(text = loginStatusText, style = MaterialTheme.typography.bodySmall)
        }

        // 覆盖层 WebView：Auto 时隐藏但继续跑；Manual 时覆盖全屏供用户操作。
        if (webLoginMode != WebLoginMode.Hidden) {
            Box(
                modifier = Modifier
                    .wrapContentSize()
                    .zIndex(10f)
            ) {
                SmartLoginWebView(
                    username = username,
                    password = password,
                    autoSubmitEnabled = webLoginMode == WebLoginMode.Auto,
                    visible = webLoginMode == WebLoginMode.Manual,
                    onLoginSuccess = {
                        Log.e("SmartLogin", "🔥 收到成功回调！")
                        loginStatusText = "✅ 登录成功！Cookie 已获取"

                        // 关键：先把“本次成功来自自动还是手动”固定下来，避免后面把 webLoginMode 设 Hidden 后分支丢失。
                        val successMode = webLoginMode

                        scope.launch {
                            when (successMode) {
                                WebLoginMode.Auto -> {
                                    viewModel.persistCredentials(
                                        rememberPassword = rememberPassword,
                                        username = username,
                                        password = password
                                    )
                                }

                                WebLoginMode.Manual -> {
                                    viewModel.persistUsernameOnlyWhenRememberEnabled(
                                        rememberPassword = rememberPassword,
                                        username = username
                                    )
                                }

                                WebLoginMode.Hidden -> Unit
                            }

                            // UI 行为：未勾选记住密码时，清空输入框内容
                            if (!rememberPassword) {
                                username = ""
                                password = ""
                            }
                        }

                        webLoginMode = WebLoginMode.Hidden

                        // ✅ 新逻辑：先尝试首次同步（失败也不闪退），然后进入主界面。
                        viewModel.onLoginSuccessFetchThenNavigate(onLoginSuccess)
                    },
                    onLoginFailed = { error ->
                        Log.e("SmartLogin", "☠️ 收到失败回调：$error")
                        loginStatusText = "❌ 自动登录失败：$error\n请在页面中手动完成登录"
                        webLoginMode = WebLoginMode.Manual
                    }
                )
            }
        }
    }
}