# 服务器版下载源

> JMCL 当前仅支持 Client 下载。本文档记录可集成的 Minecraft 服务器版软件下载源。

---

## 1. 原版 Mojang Server JAR

每个 Minecraft 版本的 `version.json` 中均包含 server 下载条目：

```
downloads.server.url → minecraft_server.{version}.jar
```

| 源 | URL 模板 |
|---|---|
| Mojang 官方 | `https://piston-data.mojang.com/v1/objects/{hash}/server.jar` |
| BMCLAPI 镜像 | `https://bmclapi2.bangbang93.com/versions/{version}/server` |

集成方式：在 `Version.getDownloadInfo()` 中增加 `DownloadType.SERVER` 分支即可。

---

## 2. Paper（高性能服务端，Spigot 替代）

| 用途 | URL |
|---|---|
| API 基础 | `https://papermc.io/api/v2` |
| 项目列表 | `https://papermc.io/api/v2/projects` |
| 版本列表 | `https://papermc.io/api/v2/projects/{project}/versions/{mcVersion}` |
| 构建下载 | `https://papermc.io/api/v2/projects/{project}/versions/{mcVersion}/builds/{build}` |

Paper 是 Spigot 的高性能 fork，插件生态兼容 Bukkit/Spigot。

---

## 3. Purpur（Paper fork，更多配置）

| 用途 | URL |
|---|---|
| API 基础 | `https://api.purpurmc.org/v2` |
| 元数据 | `https://api.purpurmc.org/v2/purpur/{mcVersion}` |
| 最新构建 | `https://api.purpurmc.org/v2/purpur/{mcVersion}/latest` |
| 下载 JAR | `https://api.purpurmc.org/v2/purpur/{mcVersion}/latest/download` |

API 返回 JSON，含 `build`、`md5`、`downloads.application.name` 等字段。

---

## 4. Folia（Paper 多线程分支）

| 用途 | URL |
|---|---|
| 版本列表 | `https://papermc.io/api/v2/projects/folia/versions/{mcVersion}` |
| 构建下载 | `https://papermc.io/api/v2/projects/folia/versions/{mcVersion}/builds/{build}` |

与 Paper 共用同一 API 格式，项目名为 `folia`。

---

## 5. Forge Server

| 用途 | URL |
|---|---|
| 版本列表 | `https://jmcl.glavo.site/metadata/forge/`（JMCL 自建） |
| 官方安装器 | `https://maven.minecraftforge.net/net/minecraftforge/forge/{ver}/forge-{ver}-installer.jar` |
| BMCLAPI 镜像 | `https://bmclapi2.bangbang93.com/forge/download?mcversion={mc}&version={forge}` |
| 安装参数 | `java -jar forge-installer.jar --installServer` |

已有 Forge 客户端安装流程，增加 `--installServer` 参数即可支持服务端安装。

---

## 6. NeoForge Server

| 用途 | URL |
|---|---|
| 版本列表 | `https://maven.neoforged.net/api/maven/versions/releases/net/neoforged/neoforge` |
| 安装器下载 | `https://maven.neoforged.net/releases/net/neoforged/neoforge/{ver}/neoforge-{ver}-installer.jar` |
| BMCLAPI 镜像 | `https://bmclapi2.bangbang93.com/neoforge/version/{ver}/download/installer.jar` |

与 Forge 类似，已有客户端流程，加 server 安装参数。

---

## 7. Fabric Server

| 用途 | URL |
|---|---|
| Loader 版本 | `https://meta.fabricmc.net/v2/versions/loader` |
| 游戏版本 | `https://meta.fabricmc.net/v2/versions/game` |
| Launch Meta | `https://meta.fabricmc.net/v2/versions/loader/{game}/{loader}/profile/server` |

已有 Fabric 客户端 API 集成，加 `/profile/server` 端点即可支持服务端。

---

## 8. Quilt Server

| 用途 | URL |
|---|---|
| Loader 版本 | `https://meta.quiltmc.org/v3/versions/loader` |
| 游戏版本 | `https://meta.quiltmc.org/v3/versions/game` |
| Launch Meta | `https://meta.quiltmc.org/v3/versions/loader/{game}/{loader}/profile/server` |

同理，已有客户端 API 集成。

---

## 9. Velocity（现代代理端，BungeeCord 替代）

| 用途 | URL |
|---|---|
| 项目列表 | `https://papermc.io/api/v2/projects/velocity` |
| 版本/构建 | `https://papermc.io/api/v2/projects/velocity/versions/{mcVersion}/builds/{build}` |

---

## 10. Legacy 服务端

| 软件 | URL |
|---|---|
| Legacy Fabric Server | `https://meta.legacyfabric.net/v2/versions/loader/{game}/{loader}/profile/server` |
| BungeeCord | `https://ci.md-5.net/job/BungeeCord/lastSuccessfulBuild/artifact/bootstrap/target/BungeeCord.jar` |
| Spigot（旧） | `https://hub.spigotmc.org/versions/`（BuildTools，需本地编译） |

---

## 集成建议

| 优先级 | 软件 | 理由 |
|---|---|---|
| P0 | 原版 Server JAR | 只需改 `getDownloadInfo()`，零额外依赖 |
| P1 | Paper | 最流行的服务端，REST API 简洁 |
| P2 | Fabric / Quilt Server | 已有客户端 API 集成 |
| P3 | Forge / NeoForge Server | 已有客户端安装流程 |
| P4 | Purpur / Folia / Velocity | Paper 变体，API 模式一致 |
