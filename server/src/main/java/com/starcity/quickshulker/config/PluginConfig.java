package com.starcity.quickshulker.config;

import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

public class PluginConfig {

    private final JavaPlugin plugin;

    private boolean rightClickToOpen;
    private boolean playSound;
    private boolean commandOpen;
    private String noPermissionMessage;
    private String reloadMessage;
    private String notShulkerMessage;

    public PluginConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        rightClickToOpen = plugin.getConfig().getBoolean("right-click-to-open", true);
        playSound = plugin.getConfig().getBoolean("play-sound", true);
        commandOpen = plugin.getConfig().getBoolean("command-open", true);
        noPermissionMessage = colorize(plugin.getConfig().getString("no-permission-message", "&c你没有权限使用此功能"));
        reloadMessage = colorize(plugin.getConfig().getString("reload-message", "&a配置已重载"));
        notShulkerMessage = colorize(plugin.getConfig().getString("not-shulker-message", "&c你手中没有潜影盒"));
    }

    private String colorize(String msg) {
        if (msg == null) return "";
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    public boolean isRightClickToOpen() { return rightClickToOpen; }
    public boolean isPlaySound() { return playSound; }
    public boolean isCommandOpen() { return commandOpen; }
    public String getNoPermissionMessage() { return noPermissionMessage; }
    public String getReloadMessage() { return reloadMessage; }
    public String getNotShulkerMessage() { return notShulkerMessage; }
}
