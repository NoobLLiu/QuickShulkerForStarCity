package com.starcity.quickshulker.listener;

import com.starcity.quickshulker.QuickShulkerPlugin;
import com.starcity.quickshulker.config.GrowthUnlockManager;
import com.starcity.quickshulker.handler.OpenHandler;
import com.starcity.quickshulker.registry.OpenableRegistry;
import org.bukkit.entity.Player;
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
        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(message));
            int windowSlot = in.readInt();

            // 解析为玩家背包索引。
            // 打印机只在主背包区(窗口槽位 9-35)自动取料，此时窗口槽位号 == 玩家背包索引；
            // 额外兼容热键(0-8)与副手(40)。
            int invSlot = resolveInventorySlot(windowSlot);
            if (invSlot < 0) {
                plugin.getLogger().info("忽略无法解析的潜影盒打开请求，窗口槽位: " + windowSlot);
                return;
            }

            ItemStack item = getItemInSlot(player, invSlot);
            if (item == null || !registry.isOpenable(item)) {
                return;
            }

            // 成长值门槛检查
            if (!growthUnlockManager.checkAndNotify(player)) {
                return;
            }

            openHandler.openShulker(player, item, invSlot);
        } catch (IOException e) {
            plugin.getLogger().warning("处理 quickshulker:open_shulker_packet 时出错: " + e.getMessage());
        }
    }

    /**
     * 将玩家背包界面窗口槽位号映射为玩家背包索引；无效/盔甲区返回 -1。
     */
    private int resolveInventorySlot(int windowSlot) {
        if (windowSlot >= 0 && windowSlot <= 35) {
            return windowSlot; // 主背包 + 快捷栏，窗口槽位号与背包索引一致
        }
        if (windowSlot == 40) {
            return 40;        // 副手
        }
        return -1;            // 盔甲区等：无法打开潜影盒
    }

    private ItemStack getItemInSlot(Player player, int slot) {
        if (slot == 40) {
            return player.getInventory().getItemInOffHand();
        }
        return player.getInventory().getItem(slot);
    }
}