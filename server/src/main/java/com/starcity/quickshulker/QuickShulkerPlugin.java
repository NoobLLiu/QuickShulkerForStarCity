package com.starcity.quickshulker;

import com.starcity.quickshulker.command.QuickShulkerCommand;
import com.starcity.quickshulker.config.ClickOpenManager;
import com.starcity.quickshulker.config.PluginConfig;
import com.starcity.quickshulker.handler.OpenHandler;
import com.starcity.quickshulker.listener.InventoryListener;
import com.starcity.quickshulker.listener.KyrptonaughtPacketListener;
import com.starcity.quickshulker.listener.PlayerInteractListener;
import com.starcity.quickshulker.listener.PluginMessageListener;
import com.starcity.quickshulker.listener.ShulkerClickOpenListener;
import com.starcity.quickshulker.registry.OpenableRegistry;
import org.bukkit.plugin.java.JavaPlugin;

public class QuickShulkerPlugin extends JavaPlugin {

    private static QuickShulkerPlugin instance;
    private PluginConfig pluginConfig;
    private ClickOpenManager clickOpenManager;
    private OpenableRegistry openableRegistry;
    private OpenHandler openHandler;

    // kyrptonaught quickshulker 协议：litematica-printer(INVOKE 方案) 打开的频道
    private static final String KYRPTONAUGHT_OPEN_SHULKER_CHANNEL = "quickshulker:open_shulker_packet";

    @Override
    public void onEnable() {
        instance = this;

        // 加载配置
        saveDefaultConfig();
        pluginConfig = new PluginConfig(this);
        clickOpenManager = new ClickOpenManager(this);

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

        // 注册插件消息通道 (客户端Mod高级功能)
        getServer().getMessenger().registerIncomingPluginChannel(this,
                pluginConfig.getPluginMessageChannel(), new PluginMessageListener(this, openHandler, openableRegistry));
        getServer().getMessenger().registerOutgoingPluginChannel(this,
                pluginConfig.getPluginMessageChannel());

        // 注册 kyrptonaught quickshulker 协议（litematica-printer INVOKE 方案）
        getServer().getMessenger().registerIncomingPluginChannel(this,
                KYRPTONAUGHT_OPEN_SHULKER_CHANNEL,
                new KyrptonaughtPacketListener(this, openHandler, openableRegistry));

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
        reloadConfig();
        pluginConfig = new PluginConfig(this);
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
}
