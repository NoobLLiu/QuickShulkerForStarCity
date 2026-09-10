package com.starcity.quickshulker;

import com.starcity.quickshulker.command.QuickShulkerCommand;
import com.starcity.quickshulker.config.PluginConfig;
import com.starcity.quickshulker.handler.OpenHandler;
import com.starcity.quickshulker.listener.InventoryListener;
import com.starcity.quickshulker.listener.PlayerInteractListener;
import com.starcity.quickshulker.listener.PluginMessageListener;
import com.starcity.quickshulker.registry.OpenableRegistry;
import org.bukkit.plugin.java.JavaPlugin;

public class QuickShulkerPlugin extends JavaPlugin {

    private static QuickShulkerPlugin instance;
    private PluginConfig pluginConfig;
    private OpenableRegistry openableRegistry;
    private OpenHandler openHandler;

    @Override
    public void onEnable() {
        instance = this;

        // 加载配置
        saveDefaultConfig();
        pluginConfig = new PluginConfig(this);

        // 初始化注册表
        openableRegistry = new OpenableRegistry();
        openableRegistry.registerDefaults();

        // 初始化打开处理器
        openHandler = new OpenHandler(this, pluginConfig, openableRegistry);

        // 注册事件监听器
        getServer().getPluginManager().registerEvents(
                new PlayerInteractListener(this, pluginConfig, openHandler, openableRegistry), this);
        getServer().getPluginManager().registerEvents(
                new InventoryListener(this, openHandler), this);

        // 注册命令
        QuickShulkerCommand commandExecutor = new QuickShulkerCommand(this, pluginConfig, openHandler, openableRegistry);
        getCommand("quickshulker").setExecutor(commandExecutor);
        getCommand("quickshulker").setTabCompleter(commandExecutor);

        // 注册插件消息通道 (客户端Mod高级功能)
        getServer().getMessenger().registerIncomingPluginChannel(this,
                pluginConfig.getPluginMessageChannel(), new PluginMessageListener(this, openHandler, openableRegistry));
        getServer().getMessenger().registerOutgoingPluginChannel(this,
                pluginConfig.getPluginMessageChannel());

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
