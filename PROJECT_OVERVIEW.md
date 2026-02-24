# MinePlus 项目接手文档（全量梳理）

> 生成时间：2026-02-25  
> 覆盖范围：当前仓库 `:app` 模块（Compose + Hilt + Retrofit + Room + DataStore + SecurityCrypto）

## 0. TL;DR（一句话理解这个 App）
MinePlus 是一个以 **WebView 自动登录教务系统** 获取 Cookie，再用 **Retrofit 调教务接口拉取课表**，经过解析清洗后落库到 **Room**，最终用 **Jetpack Compose 课表格子视图** 展示并支持下拉刷新的 App。

---

## 1. 模块结构与目录
- 单模块工程：`include(":app")`
- 关键目录
  - `app/src/main/java/com/cumtb/mineplus/`
    - `MainActivity.kt`：入口 Activity + Compose Navigation 路由
    - `MinePlusApp.kt`：`@HiltAndroidApp` Application
    - `di/`：Hilt Module（Network/DB）
    - `ui/`：Compose Screens + ViewModels + 通用组件
    - `data/`：Repository + Room + Retrofit models + prefs + HTML 解析
    - `util/`：工具类（时间映射）

---

## 2. 构建与依赖（Gradle）
### 2.1 版本与编译参数
- Kotlin：2.0.21
- AGP：9.0.0（来自 `libs.versions.toml`）
- Java：17
- minSdk：27 / targetSdk：36 / compileSdk：36

### 2.2 核心依赖与用途
- UI：Jetpack Compose（Material3）
- 导航：`androidx.navigation:navigation-compose`
- DI：Hilt（`com.google.dagger:hilt-android`）
- 网络：Retrofit + OkHttp + Logging Interceptor
- HTML 解析：Jsoup
- 数据库：Room（runtime/ktx/compiler）
- 偏好：DataStore Preferences
- 凭据加密：Security Crypto（EncryptedSharedPreferences + MasterKey）

> ✅ 当前已完成“全依赖统一版本管理”：`app/build.gradle.kts` 不再出现硬编码版本号，统一通过 `gradle/libs.versions.toml` 的 Version Catalog（`libs.*`）引用。
>
> ✅ `navigation-compose` 的重复依赖/版本不一致已修复：现在只保留 `libs.androidx.navigation.compose` 一处。
>
> ⚠️ Compose BOM 当前固定为 `2024.02.00`：因为项目使用的 Material3 pull-to-refresh API（`androidx.compose.material3.pulltorefresh.*`）在较新 BOM 中有破坏性变更；如需升级 BOM，需要同步迁移 `ScheduleScreen` 的下拉刷新实现。

---

## 3. App 入口、启动流程与路由设计
### 3.1 AndroidManifest
- Application：`.MinePlusApp`
- Launcher Activity：`.MainActivity`
- 权限：`INTERNET`

### 3.2 Navigation 路由
`MainActivity` 内部使用 `NavHost(startDestination = "splash")`，定义 3 个路由：
- `splash`：启动页，判断是否可直达课表
- `login`：登录页（WebView 自动填表登录）
- `schedule`：课表页（周次 pager + 格子课表 + 下拉刷新）

回退栈策略：
- 从 `splash` 导航到 `login/schedule` 时 `popUpTo("splash") { inclusive = true }`
- 登录成功到 `schedule` 时 `popUpTo("login") { inclusive = true }`

---

## 4. 功能清单（按用户路径）
### 4.1 Splash：自动跳转
- 逻辑：`SplashViewModel.shouldGoSchedule()`
- 条件：
  1) `AppPreferences.rememberPassword == true`
  2) `CredentialStorage.hasCredentials() == true`
- 满足则直接进 `schedule`，否则进 `login`

### 4.2 Login：WebView 自动登录 + 持久化凭据 + 拉取课表
- UI：用户名/密码输入、记住密码开关
- 自动回填：`MainViewModel.loadSavedCredentials()`
- 点击登录后展示 `SmartLoginWebView`：
  - 加载统一认证 URL
  - `evaluateJavascript` 注入脚本：寻找用户名/密码/按钮元素并自动提交
  - URL 命中 `/student/home` 或 `index` 认为登录成功 -> 回调 `onLoginSuccess()`
- 登录成功后：
  - `MainViewModel.persistCredentials()`：
    - remember=true：写 DataStore + 加密保存用户名密码
    - remember=false：写 DataStore + 清空加密存储
  - `MainViewModel.testFetchData()`：触发 `CourseRepository.refreshAllData()` 拉全量课表并落库，然后导航到 `schedule`

### 4.3 Schedule：数据库驱动课表展示 + 周次选择 + 下拉刷新
- 数据来自 Room：`CourseDao.getSchedulesByWeek(weekIndex)` -> `Flow<List<CourseSchedule>>`
- 顶部周次：`HorizontalPager`（page 0 对应 week 1）
- 下拉刷新：Material3 pull-to-refresh；触发 `ScheduleViewModel.onRefreshTriggered()` -> `repository.refreshAllData()`
- 当前周自动定位：监听 `AppPreferences.semesterStartDate/totalWeeks`，以 `startDate` 与 `LocalDate.now()` 计算当前周次

---

## 5. 分层架构与职责
### 5.1 UI 层（Compose Screens）
- `SplashScreen`：只负责展示 loading + 调起导航回调
- `LoginScreen`：输入状态 + 展示 WebView + 触发 ViewModel 行为
- `ScheduleScreen`：订阅 ViewModel StateFlow 并渲染

### 5.2 ViewModel 层
- `SplashViewModel`：判断是否可跳过登录
- `MainViewModel`：凭据读写 + 登录后触发全量刷新
- `ScheduleViewModel`：
  - 当前周/总周/加载态 StateFlow
  - 从 Dao 拿 scheduleFlow
  - 监听学期开始日期自动计算当前周

### 5.3 Data 层（核心业务）
- `CourseRepository`：一次性 ETL：
  1) 拉课表首页 HTML
  2) 从 HTML 解析 `semesterId`、`stdPersonId`
  3) 调 JSON 接口拉课程列表 + 调 datum 拉排课
  4) 映射到 Room Entities（CourseEntity/ScheduleEntity）
  5) 清库并写入
  6) 保存学期开始日期与总周次到 DataStore

---

## 6. 网络层（Retrofit）与会话设计
### 6.1 baseUrl
- `NetworkModule.provideRetrofit()`：`https://jwxt.cumtb.edu.cn/`

### 6.2 Cookie/Session
- `WebViewCookieJar`：OkHttp 的 CookieJar 与 Android `CookieManager` 打通
  - WebView 登录后 Cookie 进入系统 CookieManager
  - Retrofit 请求时从 CookieManager 读 Cookie 自动携带
  - Retrofit 响应若 Set-Cookie，则反向写入 CookieManager

### 6.3 SchoolApi 端点
- `GET student/for-std/course-table`：获取 HTML（解析学期与 personId）
- `GET student/for-std/course-table/get-data?semesterId=...&bizTypeId=2`：课程列表/周次信息
- `POST student/ws/schedule-table/datum`：提交 `DatumRequest(lessonIds,stdPersonId)` 获取排课列表

---

## 7. 数据模型（DTO）与返回结构
位于 `com.cumtb.mineplus.data.model`：
- `CourseResponse`：课程列表、currentWeek、weekIndices...
- `ScheduleResponse`：`result.scheduleList`...
- `DatumRequest`：请求体（lessonIds + stdPersonId）

> 该部分建议后续补充：字段含义、与 Room/ UI 模型的映射表。

---

## 8. 本地存储
### 8.1 Room 数据库
- DB：`AppDatabase(version=1)`，表：`courses`、`schedules`
- CourseEntity（courses）：
  - 主键 `lessonId`
  - name/teacher/credit/colorIndex/rawScheduleText
- ScheduleEntity（schedules）：
  - 自增主键 id
  - 外键 lessonId -> courses.lessonId（CASCADE）
  - weekIndex/dayOfWeek/startNode/step/room/rawStartTime/rawEndTime/date
- 核心查询：`CourseDao.getSchedulesByWeek(weekIndex)` INNER JOIN 输出 UI 聚合模型 `CourseSchedule`

### 8.2 DataStore（AppPreferences）
- `semester_start_date`：学期第 1 周周一（ISO 日期字符串）
- `total_weeks`：总周数
- `remember_password`：是否记住密码

### 8.3 EncryptedSharedPreferences（CredentialStorage）
- 加密保存 username/password
- 只在 remember_password=true 时保留

---

## 9. 核心数据流（从启动到课表）
1) 启动 -> `SplashScreen`
2) `SplashViewModel.shouldGoSchedule()`：
   - true：进 `schedule`
   - false：进 `login`
3) 登录：`SmartLoginWebView` 自动提交统一认证
4) 登录成功：保存凭据（可选）
5) `CourseRepository.refreshAllData()`：
   - HTML -> 解析 semesterId/personId
   - JSON -> 课程列表 + datum 排课
   - 映射实体 -> 清库 -> 插入
   - 保存学期开始日期/总周次
6) `ScheduleScreen` 订阅 Room Flow 自动展示

---

## 10. 已发现的风险点 / TODO（建议优先级）
### P0（高优先级）
- `CourseRepository` 解析了 `semesterId` 但调用 `getScheduleData(281)` 硬编码：应替换为解析结果并做容错。
- `app/build.gradle.kts` 中 `navigation-compose` 存在重复 & 版本不一致：建议只保留一种（推荐完全使用 Version Catalog）。
- `SmartLoginWebView` 当前 `Modifier.alpha(1f)` 实际未隐藏，且在登录页覆盖全屏；如果本意是“几乎透明但可执行 JS”，建议改回 `0.01f` 并验证交互。

### P1（中优先级）
- `fallbackToDestructiveMigration()` 会在表结构变更时清空数据：生产环境需要迁移策略。
- SSL 错误全部 `handler.proceed()` 有安全风险（仅开发环境可接受）。

### P2（建议优化）
- ScheduleScreen 左侧时间轴与右侧内容滚动不同步。
- 缺少错误上报/Toast 机制（目前只打 log）。

---

## 11. 快速上手（给新同学的 30 分钟路线）
1) 从 `MainActivity` 看路由与页面流转
2) 看 `SmartLoginWebView`：登录成功判定条件 + Cookie 策略
3) 看 `CourseRepository.refreshAllData()`：ETL 逻辑与落库
4) 看 `CourseDao.getSchedulesByWeek()`：如何把两张表 join 成 UI 模型
5) 看 `ScheduleScreen`：如何把 CourseSchedule 渲染成格子

---

## 12. 接下来我可以继续补全的内容（可选）
- 把 `CourseResponse` / `ScheduleResponse` 字段逐个解释并画映射表
- 针对登录与课表接口，补“失败原因分类 + 重试策略 + 失效重登录”建议
- 加一个最小的单元测试：HtmlParser/TimeMapper/Dao 查询映射（Robolectric/Room in-memory）
