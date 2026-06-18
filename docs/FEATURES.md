# JMCL 特色功能

> JMCL (JVM-MCL) 是一个基于 HMCL 二次开发的 Minecraft 启动器。本文档记录了 JMCL 特有的新增和改造功能。

---

## 1. 3D 皮肤渲染系统

使用 **JavaFX 原生 3D API** 渲染 Minecraft 角色皮肤，替代原 HMCL 的 2D 皮肤展示方案。不依赖 WebView/WebGL，在 macOS 上原生运行。

### 核心类

| 类 | 路径 | 作用 |
|---|---|---|
| `SkinCanvas` | `JMCL/src/main/java/org/Open_code_Studio/jmcl/ui/skin/SkinCanvas.java` | JavaFX 3D 渲染引擎，管理模型网格、材质、相机和场景 |
| `SkinViewPane` | `JMCL/src/main/java/org/Open_code_Studio/jmcl/ui/main/SkinViewPane.java` | 首页左侧的皮肤预览面板，集成 `SkinCanvas` |
| `SkinAniWavingArms` | `JMCL/src/main/java/org/Open_code_Studio/jmcl/ui/skin/animation/SkinAniWavingArms.java` | 手臂摆动动画 |
| `SkinAniRunning` | `JMCL/src/main/java/org/Open_code_Studio/jmcl/ui/skin/animation/SkinAniRunning.java` | 奔跑动画 |
| `SkinMultipleCubes` | `JMCL/src/main/java/org/Open_code_Studio/jmcl/ui/skin/SkinMultipleCubes.java` | 多层方块（外层/内层）渲染 |

### 架构

```
SkinViewPane (StackPane)
 └── SkinCanvas (Pane)
      ├── SubScene (3D 场景)
      │    ├── PerspectiveCamera (透视相机)
      │    ├── Group (root)
      │    │    ├── SkinCube 头 / 身体 / 手臂 / 腿 (PhongMaterial 材质)
      │    │    ├── SkinMultipleCubes 外层 (透明层)
      │    │    └── capeCube (披风)
      │    └── AmbientLight + PointLight (光照)
      ├── 鼠标交互: 拖拽旋转 + 滚轮缩放
      └── 动画播放器: 周期性更新骨骼位置
```

### 特点

- 支持 Alex (slim) 和 Steven (普通) 两种模型
- 支持皮肤 + 披风同时渲染
- 外层半透明层（帽子、外套等）叠加渲染
- 自动监听账户切换，更新皮肤
- 闲置时自动播放动画（手臂摆动、奔跑）

---

## 2. 实例管理器窗口

游戏启动后，自动弹出一个跟随游戏窗口的无边框管理窗口。

### 核心类

| 类 | 路径 | 作用 |
|---|---|---|
| `InstanceManagerWindow` | `JMCL/src/main/java/org/Open_code_Studio/jmcl/ui/InstanceManagerWindow.java` | 管理窗口实现 |

### 功能

- **窗口跟随**（macOS 专用）：通过 `osascript` 调用 macOS Accessibility API 实时获取游戏窗口位置，管理窗口自动贴在游戏窗口右侧
- **进程监控**：后台线程监控游戏进程，游戏退出时管理窗口自动关闭
- **操作按钮**：
  - 停止游戏
  - 打开游戏文件夹
  - 查看 Mod 列表

### 关键技术

- 使用 `Thread` + `Thread.sleep(100)` 轮询游戏窗口位置
- 解析 `osascript` 输出获取窗口 `x, y, width, height`
- 位置更新使用 `Platform.runLater()` 回到 JavaFX UI 线程
- 位置缓存避免不必要的 UI 更新

---

## 3. macOS 原生集成

通过 **JNA (Java Native Access)** 调用 macOS Objective-C Runtime，实现 JavaFX 无法直接提供的原生功能。

### 核心类

| 类 | 路径 | 作用 |
|---|---|---|
| `MacOSNativeUtils` | `JMCL/src/main/java/org/Open_code_Studio/jmcl/ui/MacOSNativeUtils.java` | macOS 原生工具类 |
| `ObjectiveCRuntime` | `JMCLCore/src/main/java/org/Open_code_Studio/jmcl/util/platform/macos/ObjectiveCRuntime.java` | Objective-C 运行时 JNA 绑定 |
| `MacOSHardwareDetector` | `JMCLCore/src/main/java/org/Open_code_Studio/jmcl/util/platform/macos/MacOSHardwareDetector.java` | macOS 硬件检测 |

### 功能

- **NSAppearance 设置**：通过 `objc_msgSend` 设置应用外观（深色/浅色）
- **窗口圆角**：调用原生 API 设置窗口圆角半径
- **硬件检测**：获取 CPU、GPU、内存等硬件信息

---

## 4. macOS 更新机制

macOS 上 `.app` 打包后，JAR 文件在 `JMCL.app/Contents/app/` 内部。更新时直接替换 JAR 文件并重启 .app。

### 核心类

| 类 | 路径 | 作用 |
|---|---|---|
| `UpdateHandler` | `JMCL/src/main/java/org/Open_code_Studio/jmcl/upgrade/UpdateHandler.java` | 更新下载与应用逻辑 |

### 流程

1. 下载更新 JAR 到临时目录
2. 检测是否运行在 macOS `.app bundle` 内
3. 解析 `.app` 路径：`.../JMCL.app/Contents/app/JVM-MCL-xxx.jar`
4. 用新 JAR 直接覆盖旧 JAR
5. 执行 `open /Applications/JMCL.app` 重启应用

### 特点

- macOS 快速更新，无需重新下载整个 .app
- 自动检测首次更新后启动，显示更新日志

---

## 5. DMG 构建系统

使用 `jpackage` + `dmgbuild` 生成 macOS 安装包。

### 脚本

| 脚本 | 路径 | 作用 |
|---|---|---|
| `build-jpackage.sh` | `build-jpackage.sh` | macOS DMG 构建脚本 |

### 流程

1. **构建 JAR**：`./gradlew clean build`
2. **生成 ICNS**：使用 `sips` + `iconutil` 生成多尺寸 ICNS（16/32/128/256/512px + @2x Retina）
3. **jpackage 打包**：生成 `.app` 目录结构
4. **dmgbuild 封装**：生成带自定义背景和多语言许可协议的 `.dmg`

### 特性

- 多语言许可协议（中文、英文）
- 自定义 DMG 背景
- 强制使用 JDK 21+ 的 jpackage

---

## 6. EXE 构建系统

### 脚本

| 脚本 | 路径 | 作用 |
|---|---|---|
| `build-ultimate.sh` | `build-ultimate.sh` | Windows EXE 构建脚本 |

### 流程

1. **Gradle 构建**：生成 shadow jar + exe
2. **图标替换**：
   - 从 JAR 中提取 `HMCLauncher.exe`
   - 编译 `CreateIcon.java`，将 `IMG_0132.JPG` 转换为 `.ico`
   - 用 Resource Hacker 替换 EXE 图标
3. **版本信息**：从 `jvmmcl.properties` 读取版本号，设置 EXE 元数据

---

## 7. Mojang 更新日志展示

首页公告栏显示最新的 Minecraft 版本更新日志。

### 核心类

| 类 | 路径 | 方法 | 作用 |
|---|---|---|---|
| `MainPage` | `JMCL/src/main/java/org/Open_code_Studio/jmcl/ui/main/MainPage.java` | `fetchMinecraftChangelogAnnouncement()` | 异步获取版本清单 |
| `MainPage` | 同上 | `createChangelogCard()` | 创建更新日志卡片 |

### 流程

1. 请求 `piston-meta.mojang.com/mc/game/version_manifest.json` 获取版本清单
2. 找到最新的正式版
3. 请求 Minecraft Wiki API 获取版本页面横幅图
4. 创建卡片：Wiki 横幅图（或渐变 fallback）+ 标题 + 版本信息 + 关闭按钮

### 特点

- 仅对未显示的版本展示（根据 `shownTips` 缓存）
- Dev 模式忽略缓存，每次启动都展示
- Dev 模式关闭不持久化
- 优先使用 Wiki 横幅图（`pithumbsize=960`），失败时使用 Minecraft 主题渐变

---

## 8. MD3 主题系统

使用 **Material Design 3** 设计规范，CSS 定义在资源文件中。

### 样式文件

| 文件 | 路径 | 作用 |
|---|---|---|
| `root.css` | `assets/css/root.css` | 主样式表 |
| 其他 CSS | `assets/css/` | 组件级样式 |

### 特点

- MD3 配色方案与圆角设计
- 皮肤预览视图专用样式
- 大圆角按钮、卡片阴影
- 公告栏、更新泡泡等自定义组件样式

---

## 9. 版本号与更新检查系统

### 版本号格式

- **正式版**: `<年份>.<主版本>.<次版本>_<构建号>` (例: `2026.1.10.1`)
- **开发版**: `DEV<发布年份>.<主版本>.<次版本>` (例: `DEV2026.1.0`)

### 更新检查

- 使用 GitHub Releases API: `https://api.github.com/repos/Open-code-Studio/JVM-MCL/releases`
- 包含预发布版本
- Dev 模式锁定到 DEVELOPMENT 频道
- 预览开关控制 Dev 版本更新提示

### 核心类

| 类 | 路径 | 作用 |
|---|---|---|
| `UpdateChecker` | `JMCL/src/main/java/org/Open_code_Studio/jmcl/upgrade/UpdateChecker.java` | 更新检查 |
| `RemoteVersion` | `JMCL/src/main/java/org/Open_code_Studio/jmcl/upgrade/RemoteVersion.java` | 远程版本信息 |

---

## 10. 完整性验证（可选）

JMCL 支持可选的完整性验证。如果没有签名文件，自动跳过验证。

- 验证逻辑根据 `RemoteVersion` 中的签名信息进行
- 签名文件不存在时自动跳过，不报错

---

## 11. 公告栏自适应布局

首页公告栏（包含公告和更新日志）使用 JavaFX `Pane` 容器实现自适应宽度。

### 技术细节

- 使用 `Pane`（非 `StackPane`）作为外层容器，不自动重排子元素
- 显式设置 `clip` 裁剪，确保内容不溢出
- 宽度绑定到父容器的 `widthProperty()`，填满全部可用宽度
- 启动按钮和更新泡泡作为 `StackPane` 叠加层浮在公告栏之上

### 核心代码

```java
Pane announcementContainer = new Pane(announcementPane);
announcementContainer.prefWidthProperty().bind(widthProperty());
announcementContainer.prefHeightProperty().bind(
    Bindings.max(heightProperty().subtract(120), 180.0)
);
FXUtils.setOverflowHidden(announcementContainer);
```

---

## 12. 导航左栏自适应隐藏

当导航到非首页页面时，左侧的 `leftCenter` 区域（含皮肤预览）自动隐藏，避免其他页面出现 260px 空白区域。

### 技术细节

- `DecoratorAnimatedPageSkin` 基类中，`leftCenter` 的可见性动态绑定到其子节点列表
- 只有调用了 `setLeftCenter()` 的页面（当前仅有 `RootPage` 首页）才显示 `leftCenter`
- 其他页面（版本管理、下载、设置等）的 `leftCenter` 自动隐藏且不占用布局空间

```java
control.leftCenter.visibleProperty().bind(
    Bindings.isNotEmpty(control.leftCenter.getChildren())
);
control.leftCenter.managedProperty().bind(control.leftCenter.visibleProperty());
```