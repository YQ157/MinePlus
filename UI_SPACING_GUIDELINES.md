# UI 间距规范指南

## 概述
本文档定义了 MinePlus 应用的统一间距系统，基于 4dp 基准单位建立清晰的层级关系，确保整个应用的视觉一致性和良好的用户体验。

## 间距系统结构

### 基础间距单位
- **基准单位**: 4dp
- **设计原则**: 所有间距值都是 4dp 的整数倍

### 间距层级分类

#### 1. 微小间距 (Tiny)
- `tiny` = 4dp - 最小可感知间距
- `tiny2` = 8dp - 双倍微小间距

**使用场景**:
- 行内文本元素间距
- 紧密排列的图标与文字
- 分割线厚度
- 边框宽度

#### 2. 小间距 (Small)
- `small` = 12dp - 用于相关元素分组
- `small2` = 16dp - 标准元素间距

**使用场景**:
- 表单标签与输入框间距
- 列表项之间的间距
- 卡片外边距
- 文本段落间距

#### 3. 中等间距 (Medium)
- `medium` = 24dp - 区块间主要间隔
- `medium2` = 32dp - 较大间距需求

**使用场景**:
- 页面主要内容区域的默认内边距
- 相邻UI元素的标准间距
- 容器内边距

#### 4. 大间距 (Large)
- `large` = 40dp - 页面级重要分隔
- `large2` = 48dp - 标准按钮高度/工具栏高度

**使用场景**:
- 按钮标准高度
- 工具栏高度
- 页面间重要分隔
- 特殊场景的大间距

#### 5. 特大间距 (Extra Large)
- `extraLarge` = 64dp - 极端场景使用

**使用场景**:
- 全屏模态对话框
- 特殊展示页面

## 页面级间距规范

### 标准页面
```kotlin
// 主内容区域内边距
modifier = Modifier.padding(Dimens.screenPadding) // 24dp

// 紧凑页面内边距  
modifier = Modifier.padding(Dimens.screenPaddingCompact) // 16dp
```

### 表单页面
```kotlin
// 表单字段间距
Spacer(modifier = Modifier.height(Dimens.formFieldSpacing)) // 16dp

// 标签与输入框间距
Spacer(modifier = Modifier.height(Dimens.formLabelSpacing)) // 8dp
```

### 列表页面
```kotlin
// 列表项内边距
modifier = Modifier.padding(Dimens.listItemPadding) // 16dp

// 列表项间距
Spacer(modifier = Modifier.height(Dimens.listItemSpacing)) // 8dp
```

## 组件特定间距

### 按钮
```kotlin
// 标准按钮尺寸
modifier = Modifier.height(Dimens.buttonHeight) // 48dp

// 按钮水平内边距
modifier = Modifier.padding(horizontal = Dimens.buttonPaddingHorizontal) // 24dp

// 按钮与其他元素间距
Spacer(modifier = Modifier.height(Dimens.buttonSpacing)) // 16dp
```

### 卡片和容器
```kotlin
// 卡片内容内边距
modifier = Modifier.padding(Dimens.cardPadding) // 16dp

// 卡片外边距
modifier = Modifier.padding(Dimens.cardMargin) // 8dp

// 容器内边距
modifier = Modifier.padding(Dimens.containerPadding) // 24dp
```

### 文本处理
```kotlin
// 行内文本元素间距
Spacer(modifier = Modifier.width(Dimens.textSpacing)) // 4dp

// 文本段落间距
Spacer(modifier = Modifier.height(Dimens.textBlockSpacing)) // 12dp
```

## 特殊用途间距

### 导航和工具栏
```kotlin
// 工具栏高度
height = Dimens.toolbarHeight // 48dp

// 应用栏标准高度
height = Dimens.appBarHeight // 56dp
```

### 网格布局
```kotlin
// 网格元素间距
modifier = Modifier.padding(Dimens.gridSpacing) // 16dp
```

### 视觉效果
```kotlin
// 圆角半径
shape = RoundedCornerShape(Dimens.cornerRadius) // 12dp

// 标准阴影高度
elevation = Dimens.elevation // 4dp
```

## 使用示例

### 登录页面典型布局
```kotlin
Column(
    modifier = Modifier
        .fillMaxSize()
        .padding(Dimens.screenPadding), // 24dp 页面内边距
    horizontalAlignment = Alignment.CenterHorizontally
) {
    Text("标题", style = MaterialTheme.typography.headlineMedium)
    
    Spacer(modifier = Modifier.height(Dimens.medium2)) // 32dp 标题间距
    
    OutlinedTextField(...)
    
    Spacer(modifier = Modifier.height(Dimens.elementSpacing)) // 16dp 字段间距
    
    OutlinedTextField(...)
    
    Spacer(modifier = Modifier.height(Dimens.small)) // 12dp 小间距
    
    // 开关组件
    Row {
        Switch(...)
        Spacer(modifier = Modifier.width(Dimens.tiny2)) // 8dp 内部间距
        Text("标签")
    }
    
    Spacer(modifier = Modifier.height(Dimens.elementSpacing)) // 16dp
    
    Button(
        modifier = Modifier.height(Dimens.buttonHeight) // 48dp 按钮高度
    ) { ... }
}
```

### 课表页面间距处理
```kotlin
// 时间轴内边距
.padding(vertical = Dimens.tiny) // 4dp

// 课程卡片外边距  
.padding(Dimens.dividerThickness) // 1dp

// 课程卡片内边距
.padding(Dimens.tiny) // 4dp

// 课程信息间距
Spacer(modifier = Modifier.height(Dimens.textSpacing)) // 4dp
```

## 注意事项

1. **一致性优先**: 同类元素使用相同的间距值
2. **层级清晰**: 重要元素使用更大的间距突出
3. **响应式考虑**: 在不同屏幕尺寸下间距应适当调整
4. **可访问性**: 确保足够的触摸目标间距
5. **性能优化**: 避免过度嵌套的 Spacer 组件

## 迁移指南

对于现有代码的迁移：

1. 识别当前使用的硬编码间距值
2. 根据间距层级选择对应的 Dimens 常量
3. 逐步替换，确保视觉效果一致
4. 测试在不同设备上的显示效果

## 维护建议

- 新增间距需求时优先使用现有常量
- 如需新增间距值，必须遵循 4dp 倍数原则
- 定期审查间距使用情况，保持系统整洁
- 团队成员应熟悉并遵守此规范