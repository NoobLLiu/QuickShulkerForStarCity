package com.starcity.quickshulker.listener;

import com.starcity.quickshulker.QuickShulkerPlugin;
import com.starcity.quickshulker.config.ClickOpenManager;
import com.starcity.quickshulker.config.GrowthUnlockManager;
import com.starcity.quickshulker.handler.OpenHandler;
import com.starcity.quickshulker.registry.OpenableRegistry;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 * 对接 litematica-printer 的 CLICK_SLOT 方案（B 方案）。
 *
 * CLICK_SLOT 模式下，打印机在玩家自己背包界面里对单个潜影盒发送一次"右键(PICKUP)"
 * 容器点击，期望服务端拦截该点击并直接打开潜影盒（而非把盒子移到光标上）。
 * 本监听将这种点击转换为打开操作，玩家无需在客户端安装任何额外模组。
 *
 * 通过 /qs clickopen 命令按玩家自由开关，默认关闭。
 * 只在玩家自己的背包界面（顶部为 CraftingInventory）拦截，不影响其它容器。
 */
public class ShulkerClickOpenListener implements Listener {

    private final QuickShulkerPlugin plugin;
    private final ClickOpenManager clickOpenManager;
    private final OpenHandler openHandler;
    private final OpenableRegistry registry;
    private final GrowthUnlockManager growthUnlockManager;

    public ShulkerClickOpenListener(QuickShulkerPlugin plugin, ClickOpenManager clickOpenManager,
                                    OpenHandler openHandler, OpenableRegistry registry) {
        this.plugin = plugin;
        this.clickOpenManager = clickOpenManager;
        this.openHandler = openHandler;
        this.registry = registry;
        this.growthUnlockManager = plugin.getGrowthUnlockManager();
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!clickOpenManager.isEnabled(player.getUniqueId())) return;
        if (!player.hasPermission("quickshulker.use")
                || !player.hasPermission("quickshulker.clickopen")) return;
        // 右键才视为"打开"手势，左键仍是拿起盒子
        if (event.getClick() != ClickType.RIGHT) return;

        // 只处理玩家自己的原版背包界面，不拦截箱子等容器里的普通右键。
        InventoryView view = event.getView();
        if (view.getType() != InventoryType.CRAFTING
                || view.getBottomInventory() != player.getInventory()) return;

        if (!(event.getClickedInventory() instanceof PlayerInventory)) return;

        ItemStack item = event.getCurrentItem();
        if (item == null || !registry.isOpenable(item)) return;

        // 成长值门槛检查
        if (!growthUnlockManager.checkAndNotify(player)) return;

        // 点击的槽位即玩家背包索引（下方区域 rawSlot 对应 PlayerInventory 0-35）
        int invSlot = event.getSlot();
        if (!isValidPlayerSlot(invSlot)) return;

        // InventoryClickEvent 期间不能直接打开另一个容器；先阻止原版点击，
        // 下一 tick 再确认槽位仍是同一个物品后执行打开。
        ItemStack clickedItem = item.clone();
        event.setCancelled(true);
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()
                    || !clickOpenManager.isEnabled(player.getUniqueId())
                    || !player.hasPermission("quickshulker.use")
                    || !player.hasPermission("quickshulker.clickopen")
                    || !growthUnlockManager.checkAndNotify(player)) {
                return;
            }

            InventoryView currentView = player.getOpenInventory();
            if (currentView.getType() != InventoryType.CRAFTING
                    || currentView.getBottomInventory() != player.getInventory()) {
                return;
            }

            ItemStack currentItem = getItemInSlot(player, invSlot);
            if (!isSameItem(clickedItem, currentItem)
                    || !registry.isOpenable(currentItem)) {
                return;
            }

            openHandler.openShulker(player, currentItem, invSlot);
        });
    }

    private static boolean isValidPlayerSlot(int slot) {
        return (slot >= 0 && slot < 36) || slot == 40;
    }

    private static ItemStack getItemInSlot(Player player, int slot) {
        if (slot == 40) {
            return player.getInventory().getItemInOffHand();
        }
        return player.getInventory().getItem(slot);
    }

    private static boolean isSameItem(ItemStack expected, ItemStack actual) {
        return expected != null
                && actual != null
                && expected.isSimilar(actual)
                && expected.getAmount() == actual.getAmount();
    }
}
