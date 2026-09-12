package com.starcity.quickshulker.listener;

import com.starcity.quickshulker.QuickShulkerPlugin;
import com.starcity.quickshulker.config.GrowthUnlockManager;
import com.starcity.quickshulker.handler.OpenHandler;
import com.starcity.quickshulker.registry.OpenableRegistry;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

/**
 * 对接 litematica-printer / kyrptonaught quickshulker 的协议（INVOKE 方案）。
 *
 * 频道: quickshulker:open_shulker_packet
 * 载荷: 1 个 int（invSlot，玩家背包界面的窗口槽位号）
 *
 * litematica-printer 的 INVOKE 模式通过 quickshulker 客户端模组
 * (ClientUtil.CheckAndSend) 发送该消息；服务端收到后解析槽位并打开对应潜影盒。
 * 复用插件现有 OpenHandler，保证写回 / 防刷逻辑一致。
 */
public class KyrptonaughtPacketListener implements org.bukkit.plugin.messaging.PluginMessageListener {

    private final QuickShulkerPlugin plugin;
    private final OpenHandler openHandler;
    private final OpenableRegistry registry;
    private final GrowthUnlockManager growthUnlockManager;

    public KyrptonaughtPacketListener(QuickShulkerPlugin plugin, OpenHandler openHandler, OpenableRegistry registry) {
        this.plugin = plugin;
        this.openHandler = openHandler;
        this.registry = registry;
        this.growthUnlockManager = plugin.getGrowthUnlockManager();
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!QuickShulkerPlugin.OPEN_SHULKER_CHANNEL.equals(channel)
                || message == null || message.length != Integer.BYTES) {
            return;
        }

        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(message));
            int windowSlot = in.readInt();

            // Paper 通常在主线程触发插件消息；若实现在线程外调用，切回服务器主线程。
            if (Bukkit.isPrimaryThread()) {
                openRequestedSlot(player, windowSlot);
            } else {
                Bukkit.getScheduler().runTask(plugin, () -> openRequestedSlot(player, windowSlot));
            }
        } catch (IOException e) {
            plugin.getLogger().warning("处理 quickshulker:open_shulker_packet 时出错: " + e.getMessage());
        }
    }

    private void openRequestedSlot(Player player, int windowSlot) {
        if (!player.isOnline()
                || player.getOpenInventory().getType() != InventoryType.CRAFTING
                || player.getOpenInventory().getBottomInventory() != player.getInventory()) {
            return;
        }

        // 解析为玩家背包索引。上游协议发送的是原版 InventoryMenu 窗口槽位：
        // 主背包 9-35、快捷栏 36-44、副手 45。
        int invSlot = resolveInventorySlot(windowSlot);
        if (invSlot < 0) {
            return;
        }

        ItemStack item = getItemInSlot(player, invSlot);
        if (item == null || !registry.isOpenable(item)) {
            return;
        }

        if (!player.hasPermission("quickshulker.use")
                || !player.hasPermission("quickshulker.open")) {
            return;
        }

        if (!growthUnlockManager.checkAndNotify(player)) {
            return;
        }

        openHandler.openShulker(player, item, invSlot);
    }

    /**
     * 将原版 InventoryMenu 槽位号映射为 Bukkit PlayerInventory 索引；无效/盔甲区返回 -1。
     */
    private int resolveInventorySlot(int windowSlot) {
        if (windowSlot >= 9 && windowSlot <= 35) {
            return windowSlot; // 玩家主背包，容器槽位与玩家背包索引一致
        }
        if (windowSlot >= 36 && windowSlot <= 44) {
            return windowSlot - 36; // 快捷栏容器槽位 -> 玩家背包索引 0-8
        }
        if (windowSlot == 45) {
            return 40; // 原版副手菜单槽位45 -> 玩家背包索引40
        }
        return -1; // 合成、盔甲等槽位不允许打开
    }

    private ItemStack getItemInSlot(Player player, int slot) {
        if (slot == 40) {
            return player.getInventory().getItemInOffHand();
        }
        return player.getInventory().getItem(slot);
    }
}
