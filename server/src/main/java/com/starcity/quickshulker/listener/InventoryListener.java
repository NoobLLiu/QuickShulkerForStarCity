package com.starcity.quickshulker.listener;

import com.starcity.quickshulker.QuickShulkerPlugin;
import com.starcity.quickshulker.handler.OpenHandler;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * 监听背包事件 - 处理潜影盒GUI的交互和关闭
 */
public class InventoryListener implements Listener {

    private final QuickShulkerPlugin plugin;
    private final OpenHandler openHandler;

    public InventoryListener(QuickShulkerPlugin plugin, OpenHandler openHandler) {
        this.plugin = plugin;
        this.openHandler = openHandler;
    }

    /**
     * 当玩家关闭GUI时，保存潜影盒内容
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        OpenHandler.OpenContext context = openHandler.getOpenContext(player.getUniqueId());
        if (context != null) {
            // 播放关闭音效
            openHandler.playCloseSound(player, context);
            // 关闭并保存
            openHandler.closeShulker(player);
        }
    }

    /**
     * 防止玩家将物品放入自己的背包时出现异常
     * 允许在潜影盒GUI内部移动物品
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        OpenHandler.OpenContext context = openHandler.getOpenContext(player.getUniqueId());
        if (context == null) return;

        // 如果点击的是潜影盒GUI区域（slot 0-26），允许操作
        // 如果点击的是玩家背包区域，也允许操作（Shift点击转移物品）
        // 我们不做额外限制，让玩家正常操作潜影盒内容
    }

    /**
     * 当玩家退出时，关闭潜影盒GUI
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        openHandler.closeShulker(event.getPlayer());
    }
}
