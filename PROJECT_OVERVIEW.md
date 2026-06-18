# MinePlus 项目总览

> 更新日期：2026-06-19
> 当前范围：`:app` 单模块 Android 工程（Kotlin + Compose + Hilt + Retrofit + Room + DataStore）

## 1. 一句话理解

MinePlus 通过 WebView 登录学校统一认证，复用 WebView Cookie 调用教务系统接口，同步并缓存课表数据，再用 Jetpack Compose 展示今日课表、周课表，并支持课前提醒。

## 2. 技术栈

- Android Gradle Plugin：9.0.0
- Gradle Wrapper：9.1.0
- Kotlin：2.0.21
- Java bytecode target：17
- compileSdk / targetSdk：36
- minSdk：27
- UI：Jetpack Compose + Material3
- DI：Hilt
- 网络：Retrofit + OkHttp + Gson converter
- HTML 解析：Jsoup
- 本地数据库：Room
- 偏好存储：DataStore Preferences
- 凭据存储：EncryptedSharedPreferences + MasterKey
- 权限引导：XXPermissions + DeviceCompat

## 3. 构建与本地环境

当前本机默认 Java 是 Temurin 26。直接运行 Gradle 会在 `:app:compileDebugJavaWithJavac` 前的 Android JDK image transform 阶段失败：

```text
Failed to transform core-for-system-modules.jar
Error while executing .../temurin-26.jdk/.../bin/jlink
```

建议使用 Android Studio 自带 JBR 21：

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest
```

当前已验证该命令通过。

## 4. 顶层路由

入口在 `MainActivity.kt`：

- `splash`：启动页，判断是否能跳过登录。
- `login`：登录页，支持自动登录与失败后的手动 WebView 登录。
- `schedule`：登录后的主界面，内部是底部导航。
- `about`：关于页。

`schedule` 路由加载 `MainScreen`，底部导航包含：

- 今日：`TodayScheduleScreen`
- 课表：`ScheduleScreen`
- 成绩：占位页
- 服务：`ServiceScreen`

服务页子路由：

- `settings`：设置页
- `reminder_settings`：课前提醒设置页

## 5. 登录与会话

登录页由 `LoginScreen` 和 `SmartLoginWebView` 组成。

自动登录流程：

1. 用户输入学号和密码。
2. `SmartLoginWebView` 加载统一认证登录页。
3. 页面加载完成后注入 JS，填充账号密码并点击登录按钮。
4. URL 命中 `/student/home` 或 `index` 时认为登录成功。
5. 如果勾选“记住密码”，凭据写入 `CredentialStorage`；否则只保留本次 Cookie。
6. 登录成功后尝试执行一次首次同步，失败也进入主界面。

Cookie 设计：

- WebView 登录后 Cookie 进入 Android `CookieManager`。
- OkHttp 使用 `WebViewCookieJar` 从 `CookieManager` 读取 Cookie。
- Retrofit 响应中的 `Set-Cookie` 会写回 `CookieManager`。

## 6. 数据同步链路

核心入口是 `CourseRepository.refreshAllData()`。

同步步骤：

1. 请求 `student/for-std/course-table` 获取 HTML。
2. 用 `HtmlParser` 解析 `semesterId` 和 `stdPersonId`。
3. 调 `get-data` 接口获取课程列表、当前周和周次范围。
4. 根据当前周反推学期第一周周一，并保存到 DataStore。
5. 调 `student/ws/schedule-table/datum` 获取排课详情。
6. 转换为 `CourseEntity` 与 `ScheduleEntity`。
7. 清空旧课程和旧排课，写入 Room。

颜色分配：

- 按课程 `lessonId` 排序后取模分配 `colorIndex`。
- 课程配色方案由 `AppPreferences.coursePaletteId` 持久化。

## 7. 本地存储

Room 数据库：

- DB 文件：`mine_plus.db`
- version：2
- 表：
  - `courses`
  - `schedules`

DAO 关键查询：

- `getSchedulesByWeek(weekIndex)`：按周查询课程聚合模型。
- `getMaxWeekIndex()`：获取最大周次。
- `getUpcomingWeekIndex(today)`：按日期定位接下来有课的周次。

DataStore：

- `app_prefs`
  - `semester_start_date`
  - `total_weeks`
  - `remember_password`
  - `course_palette`
- `reminder_settings`
  - `reminder_enabled`
- `reminder_alarm_store`
  - `alarm_ids`

加密存储：

- `CredentialStorage` 保存用户名和密码。
- 手动 WebView 登录成功时只保存用户名，不保存密码。

## 8. UI 层

### 今日课表

`TodayScheduleViewModel` 负责：

- 根据学期开始日期计算当前周。
- 根据当天星期过滤课程。
- 使用 `minuteAlignedTickerFlow` 每分钟刷新当前时间。
- 计算假期倒计时文案。
- 下拉刷新课表。

`TodayScheduleScreen` 负责：

- 顶部日期标题。
- 空状态、假期文案、重新登录/查看周课表入口。
- 课程列表与时间状态展示。
- 课程配色方案选择。

### 周课表

`ScheduleViewModel` 负责：

- 当前周、最大周、加载状态。
- 根据 active weeks 聚合周课表数据。
- 预热当前周与相邻周，减少第一次打开周课表的卡顿。
- 下拉刷新课表。
- 持久化课程配色方案。

`ScheduleScreen` 负责：

- 周次 pager。
- 表头日期和今天列高亮。
- 左侧时间轴。
- 课程卡片网格。
- 点击课程卡片后的详情浮层。

### 服务与设置

- `ServiceScreen`：设置入口、后续校园服务占位。
- `SettingsScreen`：课前提醒入口，账号相关功能仍占位。
- `ReminderSettingsScreen`：提醒开关与权限引导。
- `AboutScreen`：应用说明占位，后续可加版本号、更新日志和开源协议。

## 9. 课前提醒

主要类：

- `ReminderPreferences`：保存提醒总开关。
- `CourseReminderScheduler`：为未来 3 天课程设置 AlarmManager 闹钟。
- `ReminderAlarmStore`：保存已设置的 alarmId 集合，用于差量取消。
- `CourseReminderReceiver`：接收闹钟广播。
- `CourseNotificationHelper`：创建通知渠道并发送通知。
- `PermissionManager`：通知、精确闹钟、电池优化等权限检查和设置页跳转。

调度规则：

- 当前只保留课前提醒。
- 默认课前 15 分钟提醒。
- 如果默认提醒时间落在另一节课中，则顺延到冲突课程的下课时间。
- 每次开启提醒、刷新课表或进入主界面时会 best-effort 重新调度。

已知边界：

- 设备重启后系统闹钟会丢失，目前还没有 `BOOT_COMPLETED` 恢复逻辑。
- Android 12+ 精确闹钟权限可能不可用，会降级为非精确闹钟。
- Android 13+ 需要通知权限，否则通知不会展示。

## 10. 测试现状

已有测试：

- `TimeStatusUtilsTest`：今日课程时间状态。
- `CourseReminderSchedulerTest`：提醒时间冲突规则的纯算法用例。
- `ReminderTimeAlgorithmTest`：链式冲突相关补充用例。
- 默认模板测试：`ExampleUnitTest`、`ExampleInstrumentedTest`。

已验证命令：

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest
```

测试缺口：

- Repository 网络/解析/落库流程缺少 mock 测试。
- Room DAO 缺少 in-memory 测试。
- WebView 登录流程缺少可自动化验证。
- AlarmManager 和通知链路缺少 Android/Robolectric 测试。
- 主要 Compose 页面缺少截图或 UI 测试。

## 11. 已知风险与维护点

安全与发布：

- `MinePlusApp` 当前无条件启用 `WebView.setWebContentsDebuggingEnabled(true)`，发布前应只在 debug 构建开启。
- `SmartLoginWebView` 当前对 SSL 错误调用 `handler.proceed()`，发布前应收紧。
- OkHttp logging 当前为 `BODY`，发布前应按构建类型降级。
- 凭据虽然加密保存，但仍应避免在日志中输出敏感信息。

数据与架构：

- Room 仍使用 `fallbackToDestructiveMigration()`，生产发布前应补 migration。
- `SchoolApi.kt` 文件路径与包名不一致：文件在 `api/`，包名是 `com.cumtb.mineplus.data.api`。
- `MainActivity` 中进入 `schedule` 时直接在 composable body 调用 `scope.launch` 调度提醒，后续应改为 `LaunchedEffect`，避免重组时重复触发。
- 今日页和周课表页都有刷新逻辑，可考虑抽出统一刷新用例。

构建与依赖：

- `gradle.properties` 中多项 AGP 旧开关已提示将在 AGP 10 移除。
- `app/build.gradle.kts` 的 `kotlinOptions` 已有弃用提示，后续迁移到 `compilerOptions`。
- `SettingsScreen` 使用的 `Icons.Filled.ArrowBack` 已弃用，应改为 AutoMirrored。
- `TodayScheduleViewModel` 有 unchecked cast 警告，可拆 combine 参数消除。

## 12. 继续开发建议

近期优先级建议：

1. 先处理提醒调度的重组副作用与设备重启恢复。
2. 再收紧 WebView/SSL/logging 的发布安全边界。
3. 为 Repository、DAO、提醒调度补可重复运行的测试。
4. 完成设置页中的退出登录、账号管理和关于页信息。
5. 开始成绩查询前，先沉淀统一网络错误处理和会话过期重登录机制。
