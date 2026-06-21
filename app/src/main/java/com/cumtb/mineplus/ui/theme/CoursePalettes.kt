package com.cumtb.mineplus.ui.theme

import androidx.compose.ui.graphics.Color

object CoursePalettes {

    /** Persisted id for a course color palette. */
    enum class PaletteId(val storageKey: String, val chineseName: String) {
        TraditionalCN(storageKey = "traditional_cn", chineseName = "新中式传统色"),
        Morandi(storageKey = "morandi", chineseName = "莫兰迪柔色"),
        Macaron(storageKey = "macaron", chineseName = "马卡龙糖果色"),
        VibrantPastel(storageKey = "vibrant_pastel", chineseName = "明亮粉彩色"),
        ForestNature(storageKey = "forest_nature", chineseName = "森林自然色"),
        VintageFilm(storageKey = "vintage_film", chineseName = "复古胶片色")
    }

    val defaultPaletteId: PaletteId = PaletteId.TraditionalCN

    fun paletteIdFromStorageKey(key: String?): PaletteId {
        if (key.isNullOrBlank()) return defaultPaletteId
        return PaletteId.entries.firstOrNull { it.storageKey == key } ?: defaultPaletteId
    }

    fun colorsFor(id: PaletteId): List<Color> = when (id) {
        PaletteId.Morandi -> Morandi
        PaletteId.VibrantPastel -> VibrantPastel
        PaletteId.Macaron -> Macaron
        PaletteId.TraditionalCN -> TraditionalCN
        PaletteId.ForestNature -> ForestNature
        PaletteId.VintageFilm -> VintageFilm
    }

    // 🌫️ 莫兰迪静谧色系 (Morandi Muted)
    val Morandi = listOf(
        Color(0xFFD7CCC8), // 浅褐
        Color(0xFFBCAAA4), // 深褐
        Color(0xFFA1887F), // 咖啡
        Color(0xFF90A4AE), // 灰蓝
        Color(0xFF78909C), // 钢蓝
        Color(0xFF546E7A), // 深蓝灰
        Color(0xFF80CBC4), // 灰绿
        Color(0xFFA5D6A7), // 浅灰绿
        Color(0xFFC5E1A5), // 黄绿灰
        Color(0xFFE6EE9C), // 灰黄
        Color(0xFFF0E68C), // 卡其
        Color(0xFFFFCC80), // 杏色
        Color(0xFFFFAB91), // 灰粉
        Color(0xFFF48FB1), // 淡粉
        Color(0xFFCE93D8)  // 灰紫
    )

    // 🌈 统一质感的彩虹糖色系 (Vibrant Pastel)
    // 特点：明度统一，完美适配白色文字，色彩丰富但不杂乱
    val VibrantPastel = listOf(
        Color(0xFFEF5350), // 01. 西瓜红 (Red)
        Color(0xFFEC407A), // 02. 玫瑰粉 (Pink)
        Color(0xFFAB47BC), // 03. 葡萄紫 (Purple)
        Color(0xFF7E57C2), // 04. 深紫罗兰 (Deep Purple)
        Color(0xFF42A5F5), // 05. 天空蓝 (Blue)
        Color(0xFF29B6F6), // 06. 蔚蓝 (Light Blue)
        Color(0xFF26C6DA), // 07. 青色 (Cyan)
        Color(0xFF26A69A), // 08. 蓝绿 (Teal)
        Color(0xFF66BB6A), // 09. 草绿 (Green)
        Color(0xFF9CCC65), // 10. 嫩绿 (Light Green)
        Color(0xFFD4E157), // 11. 柠檬黄绿 (Lime) - 稍微调深了一点，保证白字可见
        Color(0xFFFFEE58), // 12. 明亮黄 (Yellow) - 必须够黄才能看清白字，或者改用黑字
        Color(0xFFFFCA28), // 13. 琥珀橙 (Amber)
        Color(0xFFFFA726), // 14. 深橙 (Orange)
        Color(0xFF8D6E63)  // 15. 暖棕 (Brown) - 唯一的暖深色，用于平衡
    )
    // 🍬 马卡龙色系 (Macaron) - 看起来比较清爽舒适
    val Macaron = listOf(
        Color(0xFFF48FB1), // 粉红
        Color(0xFFCE93D8), // 紫罗兰
        Color(0xFF9FA8DA), // 靛青
        Color(0xFF81D4FA), // 浅蓝
        Color(0xFF4DB6AC), // 蓝绿
        Color(0xFF80CBC4), // 凫绿
        Color(0xFFA5D6A7), // 浅绿
        Color(0xFFC5E1A5), // 柠檬绿
        Color(0xFFE6EE9C), // 酸橙
        Color(0xFFFFF59D), // 黄
        Color(0xFFFFCC80), // 橙
        Color(0xFFFFAB91), // 深橙
        Color(0xFFBCAAA4), // 棕
        Color(0xFFEEEEEE), // 灰
        Color(0xFFB0BEC5)  // 蓝灰
    )

    // 🏮 新中式传统色系 (Chinese Traditional Colors)
    // 特点：雅致、沉稳、自带书卷气，非常适合校园场景
    val TraditionalCN = listOf(
        Color(0xFFE6A89D), // 桃夭 (浅粉)
        Color(0xFFD48C93), // 胭脂 (深粉)
        Color(0xFFB58DB6), // 紫藤 (淡紫)
        Color(0xFF9A8FB5), // 鸢尾 (蓝紫)
        Color(0xFF8DA8B9), // 天青 (雨过天青)
        Color(0xFF7A9A98), // 艾绿 (带灰的绿)
        Color(0xFF9AB98D), // 柳染 (嫩柳绿)
        Color(0xFFC9B98D), // 缃叶 (桑芽黄)
        Color(0xFFE8C98D), // 琥珀 (暖黄)
        Color(0xFFE0A87D), // 杏子 (暖橙)
        Color(0xFFD48C7D), // 檀色 (浅棕)
        Color(0xFFB58D7D), // 赭石 (深棕)
        Color(0xFF8D9AB5), // 黛蓝 (深蓝灰)
        Color(0xFF7D8D9A), // 鼠灰 (冷灰)
        Color(0xFF9A8D8D)  // 藕荷 (灰粉)
    )

    // 🌿 森林自然色系 (Forest & Nature)
    // 特点：以绿、棕、米色为主，极度和谐，强迫症福音
    val ForestNature = listOf(
        Color(0xFFA8BFA8), // 苔藓绿
        Color(0xFF8FA88F), // 深苔绿
        Color(0xFFBFA8A8), // 枯玫瑰
        Color(0xFFA8BFBF), // 湖水绿
        Color(0xFF8FA8A8), // 灰蓝绿
        Color(0xFFBFBFA8), // 橄榄黄
        Color(0xFFA8A8BF), // 薰衣草灰
        Color(0xFF8F8FA8), // 紫杉灰
        Color(0xFFBF8F8F), // 陶土红
        Color(0xFFA88F8F), // 咖啡棕
        Color(0xFF8F8F8F), // 岩石灰
        Color(0xFFBFA88F), // 沙色
        Color(0xFF8FBF8F), // 新叶绿
        Color(0xFFA8BF8F), // 青草绿
        Color(0xFF8FA8BF)  // 远山蓝
    )

    // 🎞️ 胶片复古色系 (Vintage Film)
    // 特点：带有一点暖黄或冷青的色调偏移，像洗出来的老照片
    val VintageFilm = listOf(
        Color(0xFFD4A5A5), // 褪色红
        Color(0xFFC494B4), // 旧紫
        Color(0xFF94A4C4), // 褪色蓝
        Color(0xFF7494A4), // 青灰
        Color(0xFF84A494), // 旧绿
        Color(0xFFA4C484), // 泛黄绿
        Color(0xFFC4C484), // 陈旧黄
        Color(0xFFD4B484), // 牛皮纸
        Color(0xFFC49484), // 砖红
        Color(0xFFA48484), // 褐红
        Color(0xFF9484A4), // 灰紫
        Color(0xFF8494A4), // 铁蓝
        Color(0xFF748484), // 深灰
        Color(0xFFA4A494), // 灰绿
        Color(0xFFB4A494)  // 卡其灰
    )
    val ColorUsing = TraditionalCN
}
