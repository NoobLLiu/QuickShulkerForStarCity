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

    // MGActivitys 成长值门槛解锁
    private boolean unlockEnabled;
    private double unlockRequiredGrowth;
    private String unlockMode;
    private String lockedMessage;

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

        // MGActivitys 成长值门槛解锁配置
        unlockEnabled = plugin.getConfig().getBoolean("unlock.enabled", false);
        unlockRequiredGrowth = plugin.getConfig().getDouble("unlock.required-growth", 84200D);
        String rawMode = plugin.getConfig().getString("unlock.mode", "permanent");
        if (!"permanent".equalsIgnoreCase(rawMode) && !"dynamic".equalsIgnoreCase(rawMode)) {
            plugin.getLogger().warning("unlock.mode 值无效: " + rawMode + "，回退为 permanent");
            rawMode = "permanent";
        }
        unlockMode = rawMode.toLowerCase();
        lockedMessage = plugin.getConfig().getString("unlock.locked-message",
                "&c需要累计成长值达到 %required%，当前为 %current%。");
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

    // MGActivitys 成长值门槛解锁
    public boolean isUnlockEnabled() { return unlockEnabled; }
    public double getUnlockRequiredGrowth() { return unlockRequiredGrowth; }
    public String getUnlockMode() { return unlockMode; }
    public String getLockedMessage() { return lockedMessage; }
}
