package com.starcity.quickshulker.listener;

import com.starcity.quickshulker.QuickShulkerPlugin;
import com.starcity.quickshulker.config.PluginConfig;
import com.starcity.quickshulker.handler.OpenHandler;
import com.starcity.quickshulker.registry.OpenableRegistry;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/**
 * 监听玩家右键事件 - 右键空气打开潜影盒
 */
public class PlayerInteractListener implements Listener {

    private final QuickShulkerPlugin plugin;
    private final PluginConfig config;
    private final OpenHandler openHandler;
    private final OpenableRegistry registry;

    public PlayerInteractListener(QuickShulkerPlugin plugin, PluginConfig config,
                                   OpenHandler openHandler, OpenableRegistry registry) {
        this.plugin = plugin;
        this.config = config;
        this.openHandler = openHandler;
        this.registry = registry;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // 只处理右键空气
        if (event.getAction() != Action.RIGHT_CLICK_AIR) {
            return;
        }

        // 检查配置是否启用
        if (!config.isRightClickToOpen()) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        // 检查是否是可打开的物品
        if (!registry.isOpenable(item)) {
            return;
        }

        // 检查权限
        if (!player.hasPermission("quickshulker.open")) {
            if (!config.getNoPermissionMessage().isEmpty()) {
                player.sendMessage(config.getNoPermissionMessage());
            }
            return;
        }

        // 打开潜影盒
        if (openHandler.openShulkerInHand(player)) {
            // 发送消息
            if (!config.getOpenMessage().isEmpty()) {
                player.sendMessage(config.getOpenMessage());
            }
            // 取消事件，防止触发其他右键行为
            event.setCancelled(true);
        }
    }
}
