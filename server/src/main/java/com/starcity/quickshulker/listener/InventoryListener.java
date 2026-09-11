package com.starcity.quickshulker.listener;

import com.starcity.quickshulker.QuickShulkerPlugin;
import com.starcity.quickshulker.handler.OpenHandler;
import com.starcity.quickshulker.util.ShulkerUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;

/**
 * 监听背包/玩家事件 - GUI打开期间的防刷逻辑
 *
 * 防刷思路：潜影盒GUI打开期间，手中的潜影盒被"冻结"——
 * 任何可能导致盒子移动/丢失/被替换的操作都被取消，
 * 保证写回目标始终是同一个盒子，从而不多出物品也不丢失物品。
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
     * 关键：只有关闭的确实是本插件打开的潜影盒GUI时才处理。
     * 切换GUI时（旧GUI先关闭、新上下文已注册），不能把新上下文误销毁，
     * 否则新GUI会处于无保护状态（刷物品漏洞）。
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        OpenHandler.OpenContext context = openHandler.getOpenContext(player.getUniqueId());
        if (context == null) return;
        if (event.getInventory() != context.getShulkerInventory()) return;

        // 播放关闭音效
        openHandler.playCloseSound(player, context);
        // 关闭并保存
        openHandler.closeShulker(player);
    }

    /**
     * GUI打开期间：取消一切涉及潜影盒的点击
     * （点击GUI内物品、点击自己背包里的潜影盒、Shift点击、数字键交换等）
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (openHandler.getOpenContext(player.getUniqueId()) == null) return;

        if (involvesShulkerBox(event)) {
            event.setCancelled(true);
        }
    }

    /**
     * 判断一次点击是否涉及潜影盒物品（覆盖17种颜色）
     */
    private boolean involvesShulkerBox(InventoryClickEvent event) {
        // 光标上的物品 / 被点击槽位的物品
        if (ShulkerUtil.isShulkerBox(event.getCursor())) return true;
        if (ShulkerUtil.isShulkerBox(event.getCurrentItem())) return true;

        ClickType click = event.getClick();
        if (click == ClickType.NUMBER_KEY || click == ClickType.SWAP_OFFHAND) {
            // 数字键1-9热键交换：检查对应快捷栏槽位
            int hotbarButton = event.getHotbarButton();
            if (hotbarButton >= 0) {
                int hotbarRawSlot = event.getView().getTopInventory().getSize() + hotbarButton;
                if (ShulkerUtil.isShulkerBox(event.getView().getItem(hotbarRawSlot))) return true;
            }
            // F键与副手交换：副手里是潜影盒则禁止
            if (ShulkerUtil.isShulkerBox(event.getWhoClicked().getInventory().getItemInOffHand())) {
                return true;
            }
        }

        return false;
    }

    /**
     * GUI打开期间：禁止拖拽潜影盒物品
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (openHandler.getOpenContext(player.getUniqueId()) == null) return;

        if (ShulkerUtil.isShulkerBox(event.getOldCursor())) {
            event.setCancelled(true);
        }
    }

    /**
     * GUI打开期间：禁止丢弃潜影盒（Q键、点击槽位外丢弃等，取消后物品自动退回背包）
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (openHandler.getOpenContext(event.getPlayer().getUniqueId()) == null) return;
        if (ShulkerUtil.isShulkerBox(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    /**
     * GUI打开期间：禁止放置潜影盒
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (openHandler.getOpenContext(event.getPlayer().getUniqueId()) == null) return;
        if (ShulkerUtil.isShulkerBox(event.getItemInHand())) {
            event.setCancelled(true);
        }
    }

    /**
     * GUI打开期间：禁止主副手交换涉及潜影盒
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (openHandler.getOpenContext(player.getUniqueId()) == null) return;
        if (ShulkerUtil.isShulkerBox(event.getMainHandItem())
                || ShulkerUtil.isShulkerBox(event.getOffHandItem())) {
            event.setCancelled(true);
        }
    }

    /**
     * 死亡时立即关闭并保存：死亡掉落物与背包共享同一物品引用，
     * 此处写回的NBT会随掉落物一起保留，不会复制也不会丢失
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        openHandler.closeShulker(event.getEntity());
    }

    /**
     * 重生时兜底清理可能残留的上下文
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        openHandler.closeShulker(event.getPlayer());
    }

    /**
     * 传送时关闭GUI（关闭事件会触发保存写回）
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        if (openHandler.getOpenContext(player.getUniqueId()) != null) {
            player.closeInventory();
        }
    }

    /**
     * 当玩家退出时，关闭潜影盒GUI并保存
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        openHandler.closeShulker(event.getPlayer());
    }
}
