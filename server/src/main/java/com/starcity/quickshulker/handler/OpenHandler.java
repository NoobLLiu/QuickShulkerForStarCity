package com.starcity.quickshulker.handler;

import com.starcity.quickshulker.QuickShulkerPlugin;
import com.starcity.quickshulker.config.PluginConfig;
import com.starcity.quickshulker.registry.OpenableData;
import com.starcity.quickshulker.registry.OpenableRegistry;
import com.starcity.quickshulker.util.ShulkerUtil;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 处理潜影盒打开/关闭逻辑
 */
public class OpenHandler {

    private final QuickShulkerPlugin plugin;
    private final PluginConfig config;
    private final OpenableRegistry registry;

    // 跟踪玩家打开的潜影盒: playerUUID -> OpenContext
    private final Map<UUID, OpenContext> openContexts = new ConcurrentHashMap<>();

    public OpenHandler(QuickShulkerPlugin plugin, PluginConfig config, OpenableRegistry registry) {
        this.plugin = plugin;
        this.config = config;
        this.registry = registry;
    }

    /**
     * 尝试打开玩家手中的潜影盒
     * @return true 如果成功打开
     */
    public boolean openShulkerInHand(Player player) {
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();

        // 优先检查主手
        if (registry.isOpenable(mainHand)) {
            return openShulker(player, mainHand, player.getInventory().getHeldItemSlot());
        }
        // 然后检查副手
        if (registry.isOpenable(offHand)) {
            return openShulker(player, offHand, 40); // 40是副手槽位
        }
        return false;
    }

    /**
     * 打开指定物品的潜影盒GUI
     */
    public boolean openShulker(Player player, ItemStack shulkerItem, int slotIndex) {
        if (!registry.isOpenable(shulkerItem)) {
            return false;
        }

        // 关闭已打开的GUI（如果有）
        closeShulker(player);

        // 获取潜影盒标题
        String title = ShulkerUtil.getShulkerTitle(shulkerItem);

        // 创建潜影盒GUI
        Inventory shulkerInv = ShulkerUtil.createShulkerInventory(shulkerItem, title);

        // 记录打开上下文
        OpenContext context = new OpenContext(player.getUniqueId(), slotIndex, shulkerItem.clone(), shulkerInv);
        openContexts.put(player.getUniqueId(), context);

        // 打开GUI
        player.openInventory(shulkerInv);

        // 播放打开音效
        if (config.isPlaySound()) {
            OpenableData data = registry.getOpenableData(shulkerItem);
            if (data != null) {
                Sound sound = data.getSound(shulkerItem, true);
                if (sound != null) {
                    player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
                }
            }
        }

        return true;
    }

    /**
     * 关闭玩家的潜影盒GUI并保存
     */
    public void closeShulker(Player player) {
        OpenContext context = openContexts.remove(player.getUniqueId());
        if (context != null) {
            saveShulkerContents(player, context);
        }
    }

    /**
     * 保存潜影盒内容到物品
     */
    private void saveShulkerContents(Player player, OpenContext context) {
        ItemStack originalItem = context.getOriginalItem();
        int slotIndex = context.getSlotIndex();
        Inventory shulkerInv = context.getShulkerInventory();

        // 获取当前槽位的物品（可能已被移动）
        ItemStack currentItem = getItemFromSlot(player, slotIndex);

        // 如果物品类型变了（被移走了），不保存
        if (currentItem == null || currentItem.getType() != originalItem.getType()) {
            return;
        }

        // 将GUI中的物品写回潜影盒NBT
        ShulkerUtil.saveShulkerContents(currentItem, shulkerInv.getContents());

        // 更新玩家背包中的物品
        setItemInSlot(player, slotIndex, currentItem);
    }

    private ItemStack getItemFromSlot(Player player, int slot) {
        if (slot == 40) {
            return player.getInventory().getItemInOffHand();
        }
        return player.getInventory().getItem(slot);
    }

    private void setItemInSlot(Player player, int slot, ItemStack item) {
        if (slot == 40) {
            player.getInventory().setItemInOffHand(item);
        } else {
            player.getInventory().setItem(slot, item);
        }
    }

    /**
     * 播放关闭音效
     */
    public void playCloseSound(Player player, OpenContext context) {
        if (config.isPlaySound() && context != null) {
            OpenableData data = registry.getOpenableData(context.getOriginalItem());
            if (data != null) {
                Sound sound = data.getSound(context.getOriginalItem(), false);
                if (sound != null) {
                    player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
                }
            }
        }
    }

    /**
     * 获取玩家的打开上下文
     */
    public OpenContext getOpenContext(UUID playerUUID) {
        return openContexts.get(playerUUID);
    }

    /**
     * 关闭所有打开的GUI
     */
    public void closeAll() {
        for (Map.Entry<UUID, OpenContext> entry : openContexts.entrySet()) {
            Player player = plugin.getServer().getPlayer(entry.getKey());
            if (player != null && player.isOnline()) {
                saveShulkerContents(player, entry.getValue());
            }
        }
        openContexts.clear();
    }

    /**
     * 打开上下文
     */
    public static class OpenContext {
        private final UUID playerUUID;
        private final int slotIndex;
        private final ItemStack originalItem;
        private final Inventory shulkerInventory;

        public OpenContext(UUID playerUUID, int slotIndex, ItemStack originalItem, Inventory shulkerInventory) {
            this.playerUUID = playerUUID;
            this.slotIndex = slotIndex;
            this.originalItem = originalItem;
            this.shulkerInventory = shulkerInventory;
        }

        public UUID getPlayerUUID() { return playerUUID; }
        public int getSlotIndex() { return slotIndex; }
        public ItemStack getOriginalItem() { return originalItem; }
        public Inventory getShulkerInventory() { return shulkerInventory; }
    }
}
