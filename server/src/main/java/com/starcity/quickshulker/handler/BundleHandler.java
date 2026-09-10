package com.starcity.quickshulker.handler;

import com.starcity.quickshulker.util.ShulkerUtil;
import org.bukkit.Sound;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;

/**
 * 处理潜影盒的物品存入/取出操作
 */
public class BundleHandler {

    /**
     * 将物品存入潜影盒
     * @return 实际存入的数量
     */
    public static int insertIntoShulker(Player player, ItemStack shulkerItem, ItemStack insertItem,
                                         int shulkerSlot, int insertSlot) {
        if (shulkerItem == null || insertItem == null || insertItem.isEmpty()) return 0;
        if (!ShulkerUtil.isShulkerBox(shulkerItem)) return 0;

        // 获取潜影盒内部Inventory
        Inventory shulkerInv = getShulkerInventory(shulkerItem);
        if (shulkerInv == null) return 0;

        // 尝试将物品添加到潜影盒
        ItemStack leftover = shulkerInv.addItem(insertItem.clone()).values().stream().findFirst().orElse(null);
        int inserted = insertItem.getAmount() - (leftover != null ? leftover.getAmount() : 0);

        if (inserted > 0) {
            // 更新潜影盒内容
            saveShulkerInventory(shulkerItem, shulkerInv);
            player.getInventory().setItem(shulkerSlot, shulkerItem);

            // 减少原始物品数量
            insertItem.setAmount(insertItem.getAmount() - inserted);
            player.getInventory().setItem(insertSlot, insertItem.isEmpty() ? null : insertItem);

            // 播放音效
            player.playSound(player.getLocation(), Sound.BLOCK_SHULKER_BOX_OPEN, 0.5f, 1.0f);
        }

        return inserted;
    }

    /**
     * 从潜影盒中取出最后一个物品到指定槽位
     * @return 取出的物品，如果没有则返回null
     */
    public static ItemStack extractFromShulker(Player player, ItemStack shulkerItem,
                                                int shulkerSlot, int targetSlot) {
        if (shulkerItem == null) return null;
        if (!ShulkerUtil.isShulkerBox(shulkerItem)) return null;

        Inventory shulkerInv = getShulkerInventory(shulkerItem);
        if (shulkerInv == null) return null;

        // 找到最后一个非空格子
        ItemStack extracted = null;
        for (int i = shulkerInv.getSize() - 1; i >= 0; i--) {
            ItemStack stack = shulkerInv.getItem(i);
            if (stack != null && !stack.isEmpty()) {
                extracted = stack.clone();
                shulkerInv.setItem(i, null);
                break;
            }
        }

        if (extracted != null) {
            // 更新潜影盒内容
            saveShulkerInventory(shulkerItem, shulkerInv);
            player.getInventory().setItem(shulkerSlot, shulkerItem);

            // 将物品放入目标槽位
            ItemStack existing = player.getInventory().getItem(targetSlot);
            if (existing == null || existing.isEmpty()) {
                player.getInventory().setItem(targetSlot, extracted);
            } else {
                // 如果目标槽位有物品，尝试合并
                if (existing.isSimilar(extracted)) {
                    int maxStack = existing.getMaxStackSize();
                    int total = existing.getAmount() + extracted.getAmount();
                    if (total <= maxStack) {
                        existing.setAmount(total);
                        player.getInventory().setItem(targetSlot, existing);
                    } else {
                        existing.setAmount(maxStack);
                        player.getInventory().setItem(targetSlot, existing);
                        extracted.setAmount(total - maxStack);
                        // 多余的放回潜影盒
                        shulkerInv.addItem(extracted);
                        saveShulkerInventory(shulkerItem, shulkerInv);
                        player.getInventory().setItem(shulkerSlot, shulkerItem);
                    }
                } else {
                    // 不同物品，交换
                    player.getInventory().setItem(targetSlot, extracted);
                }
            }

            // 播放音效
            player.playSound(player.getLocation(), Sound.BLOCK_SHULKER_BOX_CLOSE, 0.5f, 1.0f);
        }

        return extracted;
    }

    /**
     * 获取潜影盒内部Inventory
     */
    private static Inventory getShulkerInventory(ItemStack shulkerItem) {
        if (!(shulkerItem.getItemMeta() instanceof BlockStateMeta meta)) return null;
        if (!(meta.getBlockState() instanceof ShulkerBox shulker)) return null;
        return shulker.getInventory();
    }

    /**
     * 保存潜影盒Inventory到物品
     */
    private static void saveShulkerInventory(ItemStack shulkerItem, Inventory inv) {
        if (!(shulkerItem.getItemMeta() instanceof BlockStateMeta meta)) return;
        if (!(meta.getBlockState() instanceof ShulkerBox shulker)) return;

        shulker.getInventory().setContents(inv.getContents());
        meta.setBlockState(shulker);
        shulkerItem.setItemMeta(meta);
    }
}
