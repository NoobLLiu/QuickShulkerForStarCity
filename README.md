# QuickShulkerForStarCity

一款 Minecraft 快捷潜影盒插件，允许玩家无需放置潜影盒即可直接打开和操作其中的物品。

包含**服务端插件**组件 — 基于 Paper API（Minecraft 1.21），所有玩家无需额外安装即可使用

## 功能

### 基础功能

| 功能 | 说明 |
|------|------|
| 右键空气打开 | 手持潜影盒右键空气即可打开，支持全部 17 种颜色 |
| 命令打开 | 使用 `/qs open` 或 `/quickshulker open` 打开手中的潜影盒 |
| 投影打印机联动 | 兼容 litematica-printer 的 `CLICK_SLOT` 和 `INVOKE` 两种快捷潜影盒模式 |
| 音效反馈 | 打开/关闭潜影盒时播放对应音效 |
| 权限控制 | 细粒度权限节点，支持与其他权限插件配合 |

## 安装

### 服务端

1. 从 [Releases](https://github.com/NoobLLiu/QuickShulkerForStarCity/releases) 下载 `QuickShulkerForStarCity-server-1.0.0.jar`
2. 放入服务器的 `plugins/` 目录
3. 重启服务器

## 命令

| 命令 | 别名 | 说明 | 权限 |
|------|------|------|------|
| `/quickshulker open` | `/qs open` | 打开手中的潜影盒 | `quickshulker.open` |
| `/quickshulker clickopen [on\|off\|toggle]` | `/qs clickopen ...` | 开关背包右键打开，供打印机 `CLICK_SLOT` 模式使用 | `quickshulker.clickopen` |
| `/quickshulker reload` | `/qs reload` | 重载配置文件 | `quickshulker.reload` |

## 权限

| 权限节点 | 说明 | 默认 |
|----------|------|------|
| `quickshulker.use` | 使用 QuickShulker 基础功能 | 所有玩家 |
| `quickshulker.open` | 打开手中的潜影盒 | 所有玩家 |
| `quickshulker.command` | 使用命令 | 所有玩家 |
| `quickshulker.clickopen` | 使用背包右键打开和打印机点击打开 | 所有玩家 |
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

# 权限不足时的消息
no-permission-message: '&c你没有权限使用此功能'

# 重载成功消息
reload-message: '&a配置已重载'

# 不是潜影盒的提示
not-shulker-message: '&c你手中没有潜影盒'
```

修改后使用 `/qs reload` 热重载配置。

### litematica-printer 联动

Paper 插件注册并接收 QuickShulker 原生协议频道
`quickshulker:open_shulker_packet`，载荷为一个大端序 `int`，因此打印机的
`INVOKE` 模式可以直接调用服务端打开潜影盒。该模式需要客户端安装打印机依赖的
QuickShulker 模组；不安装客户端模组时，请在打印机中选择 `CLICK_SLOT` 模式，并执行：

```text
/qs clickopen on
```

插件会在玩家自己的背包界面拦截打印机发送的右键容器点击，并在下一 tick 打开对应潜影盒。

## 构建

### 环境要求

- JDK 21+
- Gradle 9.0（已包含 Wrapper）

### 构建命令

```bash
# 构建服务端插件
./gradlew :server:build

# 构建全部
./gradlew build
```

构建产物：
- 服务端：`server/build/libs/QuickShulkerForStarCity-server-1.0.0.jar`

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
└── .github/workflows/build.yml      # CI/CD
```

## 许可证

[MIT](LICENSE)
