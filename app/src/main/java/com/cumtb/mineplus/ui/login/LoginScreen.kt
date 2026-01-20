package com.cumtb.mineplus.ui.login

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.cumtb.mineplus.ui.MainViewModel
import com.cumtb.mineplus.ui.components.SmartLoginWebView
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    viewModel: MainViewModel = hiltViewModel(),
    onLoginSuccess: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rememberPassword by remember { mutableStateOf(false) }

    var isLoggingIn by remember { mutableStateOf(false) }
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Mine+ 登录", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("学号") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("密码") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                autoCorrect = false
            ),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Switch(
                checked = rememberPassword,
                onCheckedChange = { rememberPassword = it }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "记住密码")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                isLoggingIn = true
                loginStatusText = "正在尝试登录..."
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = !isLoggingIn && username.isNotBlank() && password.isNotBlank()
        ) {
            if (isLoggingIn) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("登录中...")
            } else {
                Text("登录")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = loginStatusText, style = MaterialTheme.typography.bodySmall)

        if (isLoggingIn) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(40.dp)
                    .zIndex(10f)
            ) {
                SmartLoginWebView(
                    username = username,
                    password = password,
                    onLoginSuccess = {
                        Log.e("SmartLogin", "🔥 收到成功回调！")
                        loginStatusText = "✅ 登录成功！Cookie 已获取"

                        scope.launch {
                            viewModel.persistCredentials(
                                rememberPassword = rememberPassword,
                                username = username,
                                password = password
                            )
                        }

                        isLoggingIn = false
                        viewModel.testFetchData(onLoginSuccess)
                    },
                    onLoginFailed = { error ->
                        Log.e("SmartLogin", "☠️ 收到失败回调：$error")
                        loginStatusText = "❌ 登录失败: $error"
                        isLoggingIn = false
                    }
                )
            }
        }
    }
}
