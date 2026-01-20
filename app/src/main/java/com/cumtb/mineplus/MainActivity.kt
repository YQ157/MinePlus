package com.cumtb.mineplus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cumtb.mineplus.ui.login.LoginScreen
import com.cumtb.mineplus.ui.schedule.ScheduleScreen
import com.cumtb.mineplus.ui.splash.SplashScreen
import com.cumtb.mineplus.ui.theme.MinePlusTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MinePlusTheme { // 这是一个自动生成的主题包裹器
                // 1. 创建导航控制器
                val navController = rememberNavController()

                // 2. 定义导航主机，起始站是 splash
                NavHost(navController = navController, startDestination = "splash") {

                    // --- 路由定义 ---

                    // A. 启动页
                    composable("splash") {
                        SplashScreen(
                            onNavigateToLogin = {
                                // popBackStack 为了把 splash 从返回栈移除，按返回键不会回到 splash
                                navController.navigate("login") {
                                    popUpTo("splash") { inclusive = true }
                                }
                            },
                            onNavigateToSchedule = {
                                navController.navigate("schedule") {
                                    popUpTo("splash") { inclusive = true }
                                }
                            }
                        )
                    }

                    // B. 登录页
                    composable("login") {
                        LoginScreen(
                            onLoginSuccess = {
                                navController.navigate("schedule") {
                                    // 登录成功后，清空返回栈，防止按返回键回到登录页
                                    popUpTo("login") { inclusive = true }
                                }
                            }
                        )
                    }

                    // C. 课表页
                    composable("schedule") {
                        ScheduleScreen()
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MinePlusTheme {
        Greeting("Android")
    }
}