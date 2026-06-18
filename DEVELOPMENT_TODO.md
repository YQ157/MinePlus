# MinePlus 开发 TODO

> 更新日期：2026-06-19
> 说明：按“继续开发时最容易踩坑/最影响稳定性”的顺序整理。

## 已完成基线

- [x] 课表同步链路：WebView 登录、Cookie 复用、Retrofit 拉取、Room 落库。
- [x] 今日课表：按日期展示、空状态、假期倒计时、分钟级状态刷新。
- [x] 周课表：周次 pager、相邻周预热、课程卡片、课程详情浮层。
- [x] 课程配色：多套 palette，用户选择持久化。
- [x] UI 间距系统：`Dimens.kt` 与间距说明文档。
- [x] 服务/设置入口：底部导航服务页、设置页、课前提醒设置页。
- [x] 课前提醒基础版：提醒开关、权限引导、未来 3 天闹钟调度、通知展示。
- [x] 本地仓库维护：`.gitignore`、`.gitattributes`、源码控制字符清理。
- [x] 单元测试基线：时间状态与提醒时间算法测试。

## P0：近期必须处理

- [ ] 把 `MainActivity` 中进入主界面后的提醒调度改为 `LaunchedEffect`，避免 Compose 重组重复触发。
- [ ] 为课前提醒增加设备重启恢复：声明 `BOOT_COMPLETED` 权限和 receiver，开机后根据开关重新调度未来课程。
- [ ] 发布前收紧 WebView 安全：
  - [ ] `WebView.setWebContentsDebuggingEnabled(true)` 仅 debug 开启。
  - [ ] 移除或限制 SSL 错误 `handler.proceed()`。
  - [ ] OkHttp `BODY` 日志仅 debug 开启。
- [ ] 确定本地构建 JDK 策略：统一使用 Android Studio JBR 21，或在项目/IDE 文档中明确配置方式。

## P1：稳定性与架构

- [ ] Room 正式迁移：
  - [ ] 移除生产环境 `fallbackToDestructiveMigration()`。
  - [ ] 为 version 1 -> 2 补 migration。
  - [ ] 增加 Room in-memory DAO 测试。
- [ ] 会话过期处理：
  - [ ] 教务接口返回未登录/重定向时统一识别。
  - [ ] 提供重新登录提示和重试入口。
  - [ ] 避免刷新失败时只给笼统错误。
- [ ] 统一刷新用例：
  - [ ] 今日页和周课表页复用同一刷新状态和错误映射。
  - [ ] 刷新成功后统一重排提醒。
- [ ] Repository 测试：
  - [ ] mock `SchoolApi`。
  - [ ] 覆盖 HTML 解析失败、课程为空、datum 为空、写库失败。
  - [ ] 验证学期开始日期反推逻辑。
- [ ] 提醒调度测试：
  - [ ] 把提醒时间计算抽成纯函数。
  - [ ] 覆盖跨天、已过期课程、冲突顺延、权限降级。

## P2：代码卫生

- [ ] 整理 `SchoolApi.kt` 路径和包名，让路径与 `com.cumtb.mineplus.data.api` 一致。
- [ ] 修复 Gradle/AGP 弃用警告：
  - [ ] `kotlinOptions` -> `compilerOptions`。
  - [ ] 清理 `gradle.properties` 中 AGP 10 将移除的旧开关。
  - [ ] 评估 `android.dependency.excludeLibraryComponentsFromConstraints`。
- [ ] 修复 Compose / Kotlin 警告：
  - [ ] `SettingsScreen` 改用 `Icons.AutoMirrored.Filled.ArrowBack`。
  - [ ] `LoginScreen` 的 `KeyboardOptions` 改用 `autoCorrectEnabled` 构造方式。
  - [ ] `TodayScheduleViewModel` 去掉 unchecked cast。
- [ ] 精简默认模板代码：
  - [ ] 移除未使用的 `Greeting` / `GreetingPreview`。
  - [ ] 清理空 ViewModel 或占位状态，保留明确 TODO。

## P3：产品功能

- [ ] 设置页账号功能：
  - [ ] 启用退出登录。
  - [ ] 清理 Cookie、凭据、提醒闹钟和必要本地状态。
  - [ ] 明确“重新登录”和“退出登录”的区别。
- [ ] 关于页：
  - [ ] 显示版本号、构建号。
  - [ ] 增加更新日志入口。
  - [ ] 增加开源协议和隐私说明。
- [ ] 成绩查询：
  - [ ] 确认教务接口。
  - [ ] 设计成绩列表和详情模型。
  - [ ] 增加 GPA/加权统计。
- [ ] 校园服务：
  - [ ] 空闲教室。
  - [ ] 图书馆座位。
  - [ ] 校历或考试安排。

## P4：体验优化

- [ ] 课表页：
  - [ ] 课程冲突/重叠课程展示。
  - [ ] 点击课程后的详情浮层动画和可访问性。
  - [ ] 周次选择器支持“回到本周”。
- [ ] 今日页：
  - [ ] 课程列表骨架屏。
  - [ ] 网络异常空态。
  - [ ] 当前课程更醒目的视觉层级。
- [ ] 通知：
  - [ ] 通知点击进入今日课表。
  - [ ] 通知渠道设置说明。
  - [ ] 用户可配置提前提醒分钟数。

## 测试与验收清单

- [ ] 每次提交前运行：

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest
```

- [ ] 涉及 UI 时至少手动验证：
  - [ ] 首次安装 -> 登录 -> 首次同步。
  - [ ] 今日页有课/无课/开学前。
  - [ ] 周课表切周、下拉刷新、课程详情。
  - [ ] 开启/关闭课前提醒。
  - [ ] 通知权限未开、精确闹钟权限未开时的降级表现。

- [ ] 涉及登录/网络时额外验证：
  - [ ] Cookie 清理后重新登录。
  - [ ] 网络断开。
  - [ ] 教务系统会话过期。
  - [ ] 密码不保存场景。
