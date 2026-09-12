package com.starcity.quickshulker;

import com.starcity.quickshulker.command.QuickShulkerCommand;
import com.starcity.quickshulker.config.ClickOpenManager;
import com.starcity.quickshulker.config.GrowthUnlockManager;
import com.starcity.quickshulker.config.PluginConfig;
import com.starcity.quickshulker.handler.OpenHandler;
import com.starcity.quickshulker.listener.InventoryListener;
import com.starcity.quickshulker.listener.KyrptonaughtPacketListener;
import com.starcity.quickshulker.listener.PlayerInteractListener;
import com.starcity.quickshulker.listener.ShulkerClickOpenListener;
import com.starcity.quickshulker.registry.OpenableRegistry;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class QuickShulkerPlugin extends JavaPlugin {

    private static QuickShulkerPlugin instance;
    private PluginConfig pluginConfig;
    private ClickOpenManager clickOpenManager;
    private OpenableRegistry openableRegistry;
    private OpenHandler openHandler;
    private GrowthUnlockManager growthUnlockManager;

    // Kyrptonaught QuickShulker / litematica-printer INVOKE 使用的服务端频道。
    public static final String OPEN_SHULKER_CHANNEL = "quickshulker:open_shulker_packet";

    @Override
    public void onEnable() {
        instance = this;

        // 加载配置
        saveDefaultConfig();
        pluginConfig = new PluginConfig(this);
        clickOpenManager = new ClickOpenManager(this);
        growthUnlockManager = new GrowthUnlockManager(this, pluginConfig);

        // 初始化注册表
        openableRegistry = new OpenableRegistry();
        openableRegistry.registerDefaults();

        // 初始化打开处理器
        openHandler = new OpenHandler(this, pluginConfig, openableRegistry);
        // 启动GUI内容实时同步任务（防刷兜底 + 手上盒子实时刷新）
        openHandler.startSyncTask();

        // 注册事件监听器
        getServer().getPluginManager().registerEvents(
                new PlayerInteractListener(this, pluginConfig, openHandler, openableRegistry), this);
        getServer().getPluginManager().registerEvents(
                new InventoryListener(this, openHandler), this);
        // 点击打开潜影盒(B 方案，litematica-printer CLICK_SLOT)
        getServer().getPluginManager().registerEvents(
                new ShulkerClickOpenListener(this, clickOpenManager, openHandler, openableRegistry), this);

        // 注册命令
        QuickShulkerCommand commandExecutor = new QuickShulkerCommand(this, pluginConfig, openHandler,
                openableRegistry, clickOpenManager);
        getCommand("quickshulker").setExecutor(commandExecutor);
        getCommand("quickshulker").setTabCompleter(commandExecutor);

        // 注册 kyrptonaught quickshulker 协议（litematica-printer INVOKE 方案）
        KyrptonaughtPacketListener packetListener =
                new KyrptonaughtPacketListener(this, openHandler, openableRegistry);
        getServer().getMessenger().registerIncomingPluginChannel(this,
                OPEN_SHULKER_CHANNEL, packetListener);
        // 入站消息不依赖出站注册，但注册两端可以让 Paper 在握手期间明确声明该通道。
        getServer().getMessenger().registerOutgoingPluginChannel(this, OPEN_SHULKER_CHANNEL);

        // 玩家加入时检查成长值解锁状态（提示未解锁玩家）
        getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onPlayerJoin(PlayerJoinEvent event) {
                growthUnlockManager.checkAndNotify(event.getPlayer());
            }
        }, this);

        getLogger().info("QuickShulkerForStarCity 已启用!");
    }

    @Override
    public void onDisable() {
        // 关闭所有打开的潜影盒GUI
        if (openHandler != null) {
            openHandler.closeAll();
        }
        getLogger().info("QuickShulkerForStarCity 已禁用!");
    }

    public void reload() {
        if (pluginConfig != null) {
            pluginConfig.reload();
        }
    }

    public static QuickShulkerPlugin getInstance() {
        return instance;
    }

    public PluginConfig getPluginConfig() {
        return pluginConfig;
    }

    public OpenableRegistry getOpenableRegistry() {
        return openableRegistry;
    }

    public OpenHandler getOpenHandler() {
        return openHandler;
    }

    public GrowthUnlockManager getGrowthUnlockManager() {
        return growthUnlockManager;
    }
}
