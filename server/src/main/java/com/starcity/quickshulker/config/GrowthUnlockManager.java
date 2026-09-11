package com.starcity.quickshulker.config;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * MGActivitys 成长值门槛解锁管理器。
 *
 * 查询方式：调用 MGActivitys 公开 Java API（cn.gmzc.mgactivitys.api.MGActivityApi），
 * 不读取 playerdata.json、不反射、不执行控制台命令。
 *
 * 解锁模式：
 * - dynamic：每次实时查询成长值，不足即禁止。
 * - permanent：首次达标后写入本插件 data.xml，之后即使成长值下降也不重新锁定。
 */
public class GrowthUnlockManager {

    private static final String DATA_FILE = "data.yml";
    private static final String UNLOCK_PREFIX = "unlock.";

    private final JavaPlugin plugin;
    private final PluginConfig config;
    private final File dataFile;
    private final FileConfiguration data;

    // 缓存 MGActivityApi 引用；null 表示 API 不可用或未尝试加载
    private Object cachedApi;
    // 是否已尝试过获取 API（避免重复反射）
    private boolean apiAttempted;

    // 永久解锁状态缓存（UUID -> true）
    private final Map<UUID, Boolean> permanentUnlocks = new HashMap<>();

    // 最近一次查询的成长值（供外部读取，例如发奖时判断）
    private double lastCheckedGrowth;

    public GrowthUnlockManager(JavaPlugin plugin, PluginConfig config) {
        this.plugin = plugin;
        this.config = config;
        this.dataFile = new File(plugin.getDataFolder(), DATA_FILE);
        this.data = YamlConfiguration.loadConfiguration(dataFile);
        loadPermanentUnlocks();
    }

    private void loadPermanentUnlocks() {
        String section = UNLOCK_PREFIX + "permanent";
        if (data.contains(section)) {
            for (String key : data.getConfigurationSection(section).getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    if (data.getBoolean(section + "." + key, false)) {
                        permanentUnlocks.put(uuid, true);
                    }
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
    }

    /**
     * 核心检查：判断玩家是否已解锁，并在未解锁时发送提示。
     *
     * permanent 模式：若已记录永久解锁，直接返回 true（不查询 API）；
     *                 否则查询成长值，达标则记录永久解锁并返回 true。
     * dynamic 模式：  每次实时查询成长值，不足返回 false。
     *
     * @return true 表示允许使用功能，false 表示禁止
     */
    public boolean checkAndNotify(Player player) {
        if (!config.isUnlockEnabled()) {
            return true;
        }

        // permanent 模式先查缓存
        if ("permanent".equalsIgnoreCase(config.getUnlockMode())) {
            if (isPermanentlyUnlocked(player.getUniqueId())) {
                lastCheckedGrowth = Double.MAX_VALUE;
                return true;
            }
        }

        // 查询成长值
        double growth = getGrowthValue(player);
        lastCheckedGrowth = growth;

        // API 不可用（返回 -1）时视为未解锁
        if (growth < 0) {
            if (config.getUnlockEnabled()) {
                player.sendMessage(formatMessage(config.getLockedMessage(),
                        config.getUnlockRequiredGrowth(), 0));
            }
            return false;
        }

        double required = config.getUnlockRequiredGrowth();
        if (growth >= required) {
            // 达标
            if ("permanent".equalsIgnoreCase(config.getUnlockMode())) {
                savePermanentUnlock(player.getUniqueId());
            }
            return true;
        }

        // 未达标
        player.sendMessage(formatMessage(config.getLockedMessage(), required, growth));
        return false;
    }

    /**
     * 仅查询成长值是否达标（不发消息、不保存永久解锁状态）。
     * 用于外部判断"是否应该发放一次性奖励"等场景。
     *
     * @return true 表示当前成长值 >= 门槛，false 表示不足或 API 不可用
     */
    public boolean checkGrowthOnly(Player player) {
        if (!config.isUnlockEnabled()) {
            return false;
        }
        double growth = getGrowthValue(player);
        lastCheckedGrowth = growth;
        if (growth < 0) {
            return false;
        }
        return growth >= config.getUnlockRequiredGrowth();
    }

    /**
     * 手动标记玩家永久解锁（用于外部奖励发放后调用）。
     */
    public void savePermanentUnlock(UUID uuid) {
        permanentUnlocks.put(uuid, true);
        data.set(UNLOCK_PREFIX + "permanent." + uuid.toString(), true);
        saveData();
    }

    /**
     * 玩家是否已永久解锁。
     */
    public boolean isPermanentlyUnlocked(UUID uuid) {
        return permanentUnlocks.containsKey(uuid);
    }

    /**
     * 最近一次查询的成长值。
     */
    public double getLastCheckedGrowth() {
        return lastCheckedGrowth;
    }

    /**
     * 调用 MGActivitys 公开 Java API 查询累计成长值。
     * 返回 -1 表示 API 不可用或查询失败。
     */
    public double getGrowthValue(Player player) {
        Object api = getMGActivityApi();
        if (api == null) {
            return -1D;
        }
        try {
            return (double) api.getClass()
                    .getMethod("getGrowthValue", String.class)
                    .invoke(api, player.getName());
        } catch (Exception e) {
            plugin.getLogger().warning("调用 MGActivityApi.getGrowthValue 失败: " + e.getMessage());
            return -1D;
        }
    }

    /**
     * 获取 MGActivityApi 单例，带缓存和一次性日志。
     */
    private Object getMGActivityApi() {
        if (apiAttempted) {
            return cachedApi;
        }
        apiAttempted = true;
        try {
            Class<?> apiClass = Class.forName("cn.gmzc.mgactivitys.api.MGActivityApi");
            cachedApi = apiClass.getMethod("getInstance").invoke(null);
            if (cachedApi == null) {
                plugin.getLogger().warning("[MGActivitys] API 实例为 null，成长值解锁功能不可用。");
            }
        } catch (ClassNotFoundException e) {
            plugin.getLogger().info("[MGActivitys] 未检测到 MGActivitys 插件，成长值解锁功能已禁用。");
        } catch (NoClassDefFoundError e) {
            plugin.getLogger().info("[MGActivitys] MGActivitys 类加载失败（插件未正确安装），成长值解锁功能已禁用。");
        } catch (Exception e) {
            plugin.getLogger().warning("[MGActivitys] 获取 MGActivityApi 实例失败: " + e.getMessage());
        }
        return cachedApi;
    }

    /**
     * 替换消息中的占位符并上色。
     */
    private String formatMessage(String msg, double required, double current) {
        if (msg == null) return "";
        msg = msg.replace("%required%", String.format("%.0f", required));
        msg = msg.replace("%current%", String.format("%.0f", current));
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    private void saveData() {
        try {
            data.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().warning("保存成长值解锁状态时出错: " + e.getMessage());
        }
    }
}
