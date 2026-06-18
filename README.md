# MinePlus

MinePlus 是一个面向中国矿业大学（北京）教务系统的 Android 教务助手。当前核心能力是登录教务系统、同步课表、展示今日/周课表，并提供课前提醒设置。

## 当前功能

- WebView 登录统一认证，登录成功后通过 WebView Cookie 驱动 Retrofit 请求教务接口。
- 同步课程列表与排课详情，清洗后写入 Room。
- 今日课表：按日期展示当天课程，按分钟刷新“进行中 / 即将开始”等状态。
- 周课表：支持周次切换、下拉刷新、课程卡片展开、课程配色选择。
- 服务页：设置入口、课前提醒入口，成绩和校园服务暂为占位。
- 课前提醒：可开关提醒，调度未来 3 天课程闹钟，触发后展示静音高优先级通知。

## 快速开始

建议使用 Android Studio 自带 JBR 21 构建。当前本机系统默认 JDK 是 Temurin 26，直接运行 Gradle 会在 Android 36 的 JDK image 转换阶段触发 `jlink` 失败。

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew testDebugUnitTest
```

常用命令：

```bash
# 单元测试
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest

# Debug 构建
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug

# 查看工作区状态
git status --short --branch
```

## 项目结构

```text
app/src/main/java/com/cumtb/mineplus/
  MainActivity.kt                  Root Navigation
  MinePlusApp.kt                   Application / WebView Cookie 初始化
  api/                             CookieJar 与 SchoolApi 文件
  data/
    database/                      Room entities / DAO / DB
    model/                         Retrofit DTO 与 UI 聚合模型
    preference/                    DataStore 与加密凭据
    repository/                    课表同步 ETL
    scraper/                       HTML 参数解析
  di/                              Hilt modules
  notification/                    课程通知封装
  receiver/                        AlarmManager 广播接收器
  service/                         课前提醒调度与闹钟持久化
  ui/                              Compose screens / ViewModels / theme
  util/                            时间、权限等工具
```

## 关键文档

- [PROJECT_OVERVIEW.md](PROJECT_OVERVIEW.md)：当前架构、数据流、功能边界和已知风险。
- [DEVELOPMENT_TODO.md](DEVELOPMENT_TODO.md)：后续开发优先级和任务拆分。
- [UI_SPACING_GUIDELINES.md](UI_SPACING_GUIDELINES.md)：间距系统规范。
- [SPACING_REFACTOR_SUMMARY.md](SPACING_REFACTOR_SUMMARY.md)：间距重构历史记录。

## Git 与本地文件约定

- `local.properties`、`.idea/`、`.gradle/`、`build/`、`app/release/` 等本地产物不提交。
- APK/AAB、签名密钥、keystore、日志和 crash dump 已在 `.gitignore` 中忽略。
- `.gitattributes` 统一文本换行，并标记图片、jar、APK/AAB、keystore 为二进制。

## 开发注意

- `SchoolApi.kt` 当前文件路径在 `api/`，包名是 `com.cumtb.mineplus.data.api`；能编译，但后续可考虑整理路径与包名一致。
- `MinePlusApp` 当前无条件启用 WebView 调试，`SmartLoginWebView` 当前忽略 SSL 错误；发布前需要收紧。
- Room 当前使用 `fallbackToDestructiveMigration()`，数据库结构继续演进前应补正式 migration。
- 课前提醒依赖系统闹钟与通知权限；设备重启后的提醒恢复目前还没有 boot receiver。
