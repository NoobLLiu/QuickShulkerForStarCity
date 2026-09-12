package com.starcity.quickshulker.handler;

import com.starcity.quickshulker.QuickShulkerPlugin;
import com.starcity.quickshulker.config.PluginConfig;
import com.starcity.quickshulker.registry.OpenableData;
import com.starcity.quickshulker.registry.OpenableRegistry;
import com.starcity.quickshulker.util.ShulkerUtil;
import org.bukkit.Bukkit;
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

    // GUI内容同步周期（tick）：每个周期对比快照，有变化就写回盒NBT，
    // CraftBukkit会自动把NBT变化同步给客户端，实现接近实时的手上盒子刷新
    private static final long SYNC_PERIOD_TICKS = 2L;

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
     * 启动GUI内容实时同步任务（防刷兜底 + 手上盒子实时刷新）
     */
    public void startSyncTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, this::syncAll, SYNC_PERIOD_TICKS, SYNC_PERIOD_TICKS);
    }

    /**
     * 同步所有打开中的GUI：内容有变化时写回潜影盒NBT
     */
    private void syncAll() {
        for (Map.Entry<UUID, OpenContext> entry : openContexts.entrySet()) {
            Player player = plugin.getServer().getPlayer(entry.getKey());
            if (player == null || !player.isOnline()) {
                continue;
            }
            OpenContext context = entry.getValue();
            if (contentsChanged(context.getLastSavedContents(), context.getShulkerInventory().getContents())) {
                saveShulkerContents(player, context);
            }
        }
    }

    /**
     * 对比上次已保存的快照与当前GUI内容是否不同
     */
    private static boolean contentsChanged(ItemStack[] lastSaved, ItemStack[] current) {
        if (lastSaved == null) return true;
        if (lastSaved.length != current.length) return true;
        for (int i = 0; i < current.length; i++) {
            ItemStack oldStack = lastSaved[i];
            ItemStack newStack = current[i];
            boolean oldEmpty = oldStack == null || oldStack.isEmpty();
            boolean newEmpty = newStack == null || newStack.isEmpty();
            if (oldEmpty && newEmpty) continue;
            if (oldEmpty != newEmpty) return true;
            if (!oldStack.isSimilar(newStack) || oldStack.getAmount() != newStack.getAmount()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 深拷贝一份内容快照
     */
    private static ItemStack[] cloneContents(ItemStack[] contents) {
        ItemStack[] copy = new ItemStack[contents.length];
        for (int i = 0; i < contents.length; i++) {
            copy[i] = contents[i] == null ? null : contents[i].clone();
        }
        return copy;
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
        if (!isValidPlayerSlot(slotIndex) || !registry.isOpenable(shulkerItem)) {
            return false;
        }

        // 调用方传入的物品必须仍然是该玩家该槽位中的同一堆物品，
        // 避免异步/延迟打开时把内容显示给错误的盒子。
        ItemStack currentItem = getItemFromSlot(player, slotIndex);
        if (currentItem == null
                || !currentItem.isSimilar(shulkerItem)
                || currentItem.getAmount() != shulkerItem.getAmount()) {
            return false;
        }

        // 关闭已打开的GUI（如果有）
        closeShulker(player);

        // 获取潜影盒标题
        String title = ShulkerUtil.getShulkerTitle(shulkerItem);

        // 创建潜影盒GUI
        Inventory shulkerInv = ShulkerUtil.createShulkerInventory(shulkerItem, title);

        // 记录打开上下文（保存初始快照，供实时同步对比）
        OpenContext context = new OpenContext(player.getUniqueId(), slotIndex, shulkerItem.clone(), shulkerInv);
        context.setLastSavedItem(shulkerItem.clone());
        context.setLastSavedContents(cloneContents(shulkerInv.getContents()));
        openContexts.put(player.getUniqueId(), context);

        // 打开GUI；若被其他插件取消（返回null），回滚上下文避免留下无主状态
        if (player.openInventory(shulkerInv) == null) {
            openContexts.remove(player.getUniqueId());
            return false;
        }

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
        ItemStack lastSavedItem = context.getLastSavedItem();
        int slotIndex = context.getSlotIndex();
        Inventory shulkerInv = context.getShulkerInventory();

        // 获取当前槽位的物品（可能已被移动）
        ItemStack currentItem = getItemFromSlot(player, slotIndex);

        // 如果物品类型变了（被移走了），以世界现状为准：
        // 不写回、也不另找位置发放，避免把内容写进错误的盒子造成复制
        if (currentItem == null || lastSavedItem == null
                || !currentItem.isSimilar(lastSavedItem)
                || currentItem.getAmount() != lastSavedItem.getAmount()) {
            return;
        }

        // 将GUI中的物品写回潜影盒NBT
        ShulkerUtil.saveShulkerContents(currentItem, shulkerInv.getContents());

        // 更新玩家背包中的物品（同时触发客户端同步，刷新手上盒子内容）
        setItemInSlot(player, slotIndex, currentItem);

        // 刷新快照
        context.setLastSavedItem(currentItem.clone());
        context.setLastSavedContents(cloneContents(shulkerInv.getContents()));
    }

    private ItemStack getItemFromSlot(Player player, int slot) {
        if (slot == 40) {
            return player.getInventory().getItemInOffHand();
        }
        return player.getInventory().getItem(slot);
    }

    private static boolean isValidPlayerSlot(int slot) {
        return (slot >= 0 && slot < 36) || slot == 40;
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
        private ItemStack lastSavedItem;
        private ItemStack[] lastSavedContents;

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
        public ItemStack getLastSavedItem() { return lastSavedItem; }
        public void setLastSavedItem(ItemStack lastSavedItem) { this.lastSavedItem = lastSavedItem; }
        public ItemStack[] getLastSavedContents() { return lastSavedContents; }
        public void setLastSavedContents(ItemStack[] lastSavedContents) { this.lastSavedContents = lastSavedContents; }
    }
}
