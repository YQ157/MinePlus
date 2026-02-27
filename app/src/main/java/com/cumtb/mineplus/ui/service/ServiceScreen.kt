package com.cumtb.mineplus.ui.service

import androidx.compose.foundation.clickable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.RectangleShape
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.cumtb.mineplus.ui.theme.Dimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceScreen(
    navController: NavController,
    @Suppress("UNUSED_PARAMETER") viewModel: ServiceViewModel = hiltViewModel()
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "服务",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
            
            // 添加顶部分割线
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                thickness = 0.5.dp
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(Dimens.screenPaddingCompact)
        ) {
            // 设置入口卡片
            ServiceCard(
                title = "设置",
                description = "系统设置和提醒管理",
                icon = Icons.Filled.Settings,
                onClick = { navController.navigate("settings") }
            )
            
            Spacer(modifier = Modifier.height(Dimens.small2))
            
            // 其他服务功能区域标题
            Text(
                text = "校园服务",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(Dimens.tiny2))
            
            // 预留其他服务功能卡片
            ServiceCard(
                title = "空闲教室",
                description = "查询当前空闲教室信息",
                icon = Icons.Filled.Alarm, // 临时使用，后续替换为合适的图标
                enabled = false, // 暂时禁用
                onClick = { /* TODO */ }
            )
            
            Spacer(modifier = Modifier.height(Dimens.small2))
            
            ServiceCard(
                title = "图书馆座位",
                description = "在线预约图书馆座位",
                icon = Icons.Filled.Alarm, // 临时使用
                enabled = false, // 暂时禁用
                onClick = { /* TODO */ }
            )
        }
    }
}

@Composable
private fun ServiceCard(
    title: String,
    description: String,
    icon: ImageVector,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val outlineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (enabled) 0f else 0.35f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            // Disabled cards: use a thin outline so the gray edge is not "thick".
            .then(
                if (enabled) {
                    Modifier
                } else {
                    Modifier.border(
                        width = 0.5.dp,
                        color = outlineColor,
                        shape = RectangleShape
                    )
                }
            )
            .clickable(enabled = enabled) { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) {
                MaterialTheme.colorScheme.surface
            } else {
                // Keep background close to surface; avoid heavy gray fill.
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.cardPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                },
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(Dimens.small2))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    }
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    }
                )
            }
            
            if (!enabled) {
                Text(
                    text = "敬请期待",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}