package com.starcity.quickshulker.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * 管理"点击打开潜影盒"(CLICK_SLOT / litematica-printer B 方案)的每玩家开关。
 *
 * 该开关默认关闭，玩家可自行开启；开启后，在自己背包界面右键单个潜影盒
 * 即可打开（供 litematica-printer 在 CLICK_SLOT 模式下无需额外客户端模组使用）。
 * 状态持久化到 data.yml，重启后保留。
 */
public class ClickOpenManager {

    private static final String DATA_FILE = "data.yml";
    private static final String KEY_PREFIX = "click-open.";

    private final JavaPlugin plugin;
    private final File file;
    private final FileConfiguration data;

    public ClickOpenManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), DATA_FILE);
        this.data = YamlConfiguration.loadConfiguration(file);
    }

    /**
     * 是否开启点击打开（默认关闭）
     */
    public boolean isEnabled(UUID playerUUID) {
        return data.getBoolean(KEY_PREFIX + playerUUID, false);
    }

    /**
     * 设置点击打开状态并立即保存
     */
    public void setEnabled(UUID playerUUID, boolean enabled) {
        data.set(KEY_PREFIX + playerUUID, enabled);
        save();
    }

    private void save() {
        try {
            FileConfiguration latest = YamlConfiguration.loadConfiguration(file);
            for (String key : data.getKeys(true)) {
                if (key.startsWith(KEY_PREFIX) && data.getConfigurationSection(key) == null) {
                    latest.set(key, data.get(key));
                }
            }
            latest.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("保存点击打开开关状态时出错: " + e.getMessage());
        }
    }
}
