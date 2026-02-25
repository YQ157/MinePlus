package com.cumtb.mineplus.ui.theme

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 统一间距系统规范
 * 基于 4dp 基准单位的倍数关系，建立清晰的间距层级
 */
object Dimens {
    // 基础间距单位
    private const val BASE_UNIT = 4
    
    // 微小间距 - 用于紧密元素间的微调
    val tiny = (BASE_UNIT * 1).dp      // 4dp
    val tiny2 = (BASE_UNIT * 2).dp     // 8dp
    
    // 小间距 - 用于相关元素分组
    val small = (BASE_UNIT * 3).dp     // 12dp
    val small2 = (BASE_UNIT * 4).dp    // 16dp
    
    // 中等间距 - 用于区块间的主要间隔
    val medium = (BASE_UNIT * 6).dp    // 24dp
    val medium2 = (BASE_UNIT * 8).dp   // 32dp
    
    // 大间距 - 用于页面级的重要分隔
    val large = (BASE_UNIT * 10).dp    // 40dp
    val large2 = (BASE_UNIT * 12).dp   // 48dp
    
    // 特大间距 - 用于特殊场景
    val extraLarge = (BASE_UNIT * 16).dp // 64dp
    
    // 页面级间距
    val screenPadding = medium          // 24dp - 页面主要内容区域的默认内边距
    val screenPaddingCompact = small2   // 16dp - 紧凑页面的内边距
    
    // 元素间距
    val elementSpacing = small2         // 16dp - 相邻UI元素的标准间距
    val elementSpacingTight = tiny2     // 8dp - 紧密排列元素的间距
    val elementSpacingLoose = medium    // 24dp - 需要强调分离的元素间距
    
    // 文本间距
    val textSpacing = tiny              // 4dp - 行内文本元素间距
    val textBlockSpacing = small        // 12dp - 文本段落间距
    
    // 卡片和容器
    val cardPadding = small2            // 16dp - 卡片内容内边距
    val cardMargin = tiny2              // 8dp - 卡片外边距
    val containerPadding = medium       // 24dp - 容器内边距
    
    // 按钮和交互元素
    val buttonHeight = large2           // 48dp - 标准按钮高度
    val buttonPaddingHorizontal = medium // 24dp - 按钮水平内边距
    val buttonSpacing = small2          // 16dp - 按钮与其他元素间距
    
    // 表单元素
    val formFieldSpacing = small2       // 16dp - 表单字段间距
    val formLabelSpacing = tiny2        // 8dp - 标签与输入框间距
    
    // 导航和工具栏
    val toolbarHeight = large2          // 48dp - 工具栏高度
    val appBarHeight = (BASE_UNIT * 14).dp // 56dp - 应用栏标准高度
    
    // 列表和网格
    val listItemPadding = small2        // 16dp - 列表项内边距
    val listItemSpacing = tiny2         // 8dp - 列表项间距
    val gridSpacing = small2            // 16dp - 网格元素间距
    
    // 特殊用途间距
    val dividerThickness = 1.dp         // 分割线厚度
    val borderWidth = 1.dp              // 边框宽度
    val cornerRadius = small            // 12dp - 圆角半径
    val elevation = tiny                // 4dp - 标准阴影高度
}

/**
 * 字体大小规范 - 保持与Type.kt的一致性
 */
object FontDimens {
    val headlineLarge = 32.sp
    val headlineMedium = 24.sp
    val headlineSmall = 20.sp
    val titleLarge = 18.sp
    val titleMedium = 16.sp
    val titleSmall = 14.sp
    val bodyLarge = 16.sp
    val bodyMedium = 14.sp
    val bodySmall = 12.sp
    val labelLarge = 14.sp
    val labelMedium = 12.sp
    val labelSmall = 11.sp
}