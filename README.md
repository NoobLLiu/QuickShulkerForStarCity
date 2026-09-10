# QuickShulkerForStarCity

一款 Minecraft 快捷潜影盒插件，允许玩家无需放置潜影盒即可直接打开和操作其中的物品。

包含两个组件：
- **服务端插件** — 基于 Paper API（Minecraft 1.21），所有玩家无需额外安装即可使用
- **客户端 Mod**（可选） — 基于 Fabric（Minecraft 1.21.11），提供快捷键和背包内右键打开等进阶功能

## 功能

### 基础功能（仅需服务端插件）

| 功能 | 说明 |
|------|------|
| 右键空气打开 | 手持潜影盒右键空气即可打开，支持全部 17 种颜色 |
| 命令打开 | 使用 `/qs open` 或 `/quickshulker open` 打开手中的潜影盒 |
| 物品存入 | 通过客户端 Mod 插件消息将物品直接存入手中的潜影盒 |
| 物品取出 | 通过客户端 Mod 插件消息从潜影盒中取出物品 |
| 音效反馈 | 打开/关闭潜影盒时播放对应音效 |
| 权限控制 | 细粒度权限节点，支持与其他权限插件配合 |

### 进阶功能（需要客户端 Mod）

| 功能 | 说明 |
|------|------|
| 快捷键打开 | 按 `K` 键快速打开手中的潜影盒（可自定义） |
| 背包内右键 | 在背包界面中右键单个潜影盒直接打开 |

## 安装

### 服务端

1. 从 [Releases](https://github.com/NoobLLiu/QuickShulkerForStarCity/releases) 下载 `QuickShulkerForStarCity-server-1.0.0.jar`
2. 放入服务器的 `plugins/` 目录
3. 重启服务器

### 客户端（可选）

1. 下载 `QuickShulkerForStarCity-client-1.0.0.jar`
2. 放入客户端的 `mods/` 目录
3. 需要安装 [Fabric Loader](https://fabricmc.net/) 和 [Fabric API](https://modrinth.com/mod/fabric-api)

## 命令

| 命令 | 别名 | 说明 | 权限 |
|------|------|------|------|
| `/quickshulker open` | `/qs open` | 打开手中的潜影盒 | `quickshulker.open` |
| `/quickshulker reload` | `/qs reload` | 重载配置文件 | `quickshulker.reload` |

## 权限

| 权限节点 | 说明 | 默认 |
|----------|------|------|
| `quickshulker.use` | 使用 QuickShulker 基础功能 | 所有玩家 |
| `quickshulker.open` | 打开手中的潜影盒 | 所有玩家 |
| `quickshulker.command` | 使用命令 | 所有玩家 |
| `quickshulker.reload` | 重载配置 | OP |

## 配置

配置文件位于 `plugins/QuickShulkerForStarCity/config.yml`：

```yaml
# 是否允许右键空气打开潜影盒
right-click-to-open: true

# 打开/关闭潜影盒时是否播放音效
play-sound: true

# 是否允许通过命令打开
command-open: true

# 打开潜影盒时显示的消息（留空则不显示，支持 & 颜色代码）
open-message: '&a已打开潜影盒'

# 权限不足时的消息
no-permission-message: '&c你没有权限使用此功能'

# 重载成功消息
reload-message: '&a配置已重载'

# 不是潜影盒的提示
not-shulker-message: '&c你手中没有潜影盒'

# 客户端 Mod 高级功能的频道名（需要与客户端 Mod 一致）
plugin-message-channel: 'quickshulker:main'
```

修改后使用 `/qs reload` 热重载配置。

## 构建

### 环境要求

- JDK 21+
- Gradle 9.0（已包含 Wrapper）

### 构建命令

```bash
# 构建服务端插件
./gradlew :server:build

# 构建客户端 Mod
./gradlew :client:build

# 构建全部
./gradlew build
```

构建产物：
- 服务端：`server/build/libs/QuickShulkerForStarCity-server-1.0.0.jar`
- 客户端：`client/build/libs/QuickShulkerForStarCity-client-1.0.0.jar`

## 项目结构

```
QuickShulkerForStarCity/
├── server/                          # 服务端插件（Paper API）
│   └── src/main/java/com/starcity/quickshulker/
│       ├── QuickShulkerPlugin.java  # 插件主类
│       ├── command/                 # 命令处理
│       ├── config/                  # 配置管理
│       ├── handler/                 # 核心业务逻辑
│       ├── listener/                # 事件监听
│       ├── registry/                # 可打开物品注册表
│       └── util/                    # 工具类
├── client/                          # 客户端 Mod（Fabric）
│   └── src/main/java/com/starcity/quickshulker/client/
│       ├── QuickShulkerClientMod.java  # 客户端入口
│       ├── ClientPacketSender.java     # 网络包发送
│       └── mixin/                      # Mixin 注入
└── .github/workflows/build.yml      # CI/CD
```

## 许可证

[MIT](LICENSE)
