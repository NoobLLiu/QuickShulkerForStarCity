package com.starcity.quickshulker.listener;

import com.starcity.quickshulker.QuickShulkerPlugin;
import com.starcity.quickshulker.config.GrowthUnlockManager;
import com.starcity.quickshulker.config.PluginConfig;
import com.starcity.quickshulker.handler.OpenHandler;
import com.starcity.quickshulker.registry.OpenableRegistry;
import io.papermc.paper.event.player.PlayerPickItemEvent;
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
    private final GrowthUnlockManager growthUnlockManager;

    public PlayerInteractListener(QuickShulkerPlugin plugin, PluginConfig config,
                                   OpenHandler openHandler, OpenableRegistry registry) {
        this.plugin = plugin;
        this.config = config;
        this.openHandler = openHandler;
        this.registry = registry;
        this.growthUnlockManager = plugin.getGrowthUnlockManager();
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

        // 成长值门槛检查
        if (!growthUnlockManager.checkAndNotify(player)) {
            return;
        }

        // 打开潜影盒
        if (openHandler.openShulkerInHand(player)) {
            // 取消事件，防止触发其他右键行为
            event.setCancelled(true);
        }
    }

    /**
     * GUI打开期间禁止中键选取（点击选取可能把正在打开的盒子换走，导致内容串盒）
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerPickItem(PlayerPickItemEvent event) {
        if (openHandler.getOpenContext(event.getPlayer().getUniqueId()) != null) {
            event.setCancelled(true);
        }
    }
}
