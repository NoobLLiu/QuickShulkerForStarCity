package com.starcity.quickshulker.util;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;

/**
 * 潜影盒工具类
 */
public class ShulkerUtil {

    // 潜影盒材料 -> 颜色名称映射
    private static final Map<Material, String> SHULKER_NAMES = new HashMap<>();

    static {
        SHULKER_NAMES.put(Material.SHULKER_BOX, "潜影盒");
        SHULKER_NAMES.put(Material.WHITE_SHULKER_BOX, "白色潜影盒");
        SHULKER_NAMES.put(Material.ORANGE_SHULKER_BOX, "橙色潜影盒");
        SHULKER_NAMES.put(Material.MAGENTA_SHULKER_BOX, "品红色潜影盒");
        SHULKER_NAMES.put(Material.LIGHT_BLUE_SHULKER_BOX, "淡蓝色潜影盒");
        SHULKER_NAMES.put(Material.YELLOW_SHULKER_BOX, "黄色潜影盒");
        SHULKER_NAMES.put(Material.LIME_SHULKER_BOX, "黄绿色潜影盒");
        SHULKER_NAMES.put(Material.PINK_SHULKER_BOX, "粉红色潜影盒");
        SHULKER_NAMES.put(Material.GRAY_SHULKER_BOX, "灰色潜影盒");
        SHULKER_NAMES.put(Material.LIGHT_GRAY_SHULKER_BOX, "淡灰色潜影盒");
        SHULKER_NAMES.put(Material.CYAN_SHULKER_BOX, "青色潜影盒");
        SHULKER_NAMES.put(Material.PURPLE_SHULKER_BOX, "紫色潜影盒");
        SHULKER_NAMES.put(Material.BLUE_SHULKER_BOX, "蓝色潜影盒");
        SHULKER_NAMES.put(Material.BROWN_SHULKER_BOX, "棕色潜影盒");
        SHULKER_NAMES.put(Material.GREEN_SHULKER_BOX, "绿色潜影盒");
        SHULKER_NAMES.put(Material.RED_SHULKER_BOX, "红色潜影盒");
        SHULKER_NAMES.put(Material.BLACK_SHULKER_BOX, "黑色潜影盒");
    }

    /**
     * 检查物品是否是潜影盒
     */
    public static boolean isShulkerBox(ItemStack item) {
        if (item == null) return false;
        return SHULKER_NAMES.containsKey(item.getType());
    }

    /**
     * 获取潜影盒的显示标题
     */
    public static String getShulkerTitle(ItemStack shulkerItem) {
        if (shulkerItem == null) return "潜影盒";

        // 如果有自定义名称，使用自定义名称
        ItemMeta meta = shulkerItem.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            return meta.getDisplayName();
        }

        // 否则使用默认颜色名称
        return SHULKER_NAMES.getOrDefault(shulkerItem.getType(), "潜影盒");
    }

    /**
     * 创建潜影盒GUI并填充物品
     */
    public static Inventory createShulkerInventory(ItemStack shulkerItem, String title) {
        Inventory inv = Bukkit.createInventory(null, 27, title);

        if (shulkerItem == null || !isShulkerBox(shulkerItem)) {
            return inv;
        }

        ItemMeta meta = shulkerItem.getItemMeta();
        if (meta instanceof BlockStateMeta blockStateMeta) {
            if (blockStateMeta.hasBlockState()) {
                if (blockStateMeta.getBlockState() instanceof ShulkerBox shulker) {
                    ItemStack[] contents = shulker.getInventory().getContents();
                    for (int i = 0; i < Math.min(contents.length, 27); i++) {
                        if (contents[i] != null) {
                            inv.setItem(i, contents[i].clone());
                        }
                    }
                }
            }
        }

        return inv;
    }

    /**
     * 将GUI中的物品保存回潜影盒
     */
    public static void saveShulkerContents(ItemStack shulkerItem, ItemStack[] contents) {
        if (shulkerItem == null || !isShulkerBox(shulkerItem)) {
            return;
        }

        ItemMeta meta = shulkerItem.getItemMeta();
        if (meta instanceof BlockStateMeta blockStateMeta) {
            if (blockStateMeta.getBlockState() instanceof ShulkerBox shulker) {
                // 清空潜影盒
                shulker.getInventory().clear();

                // 填入新物品
                for (int i = 0; i < Math.min(contents.length, 27); i++) {
                    shulker.getInventory().setItem(i, contents[i] != null ? contents[i].clone() : null);
                }

                // 更新潜影盒状态
                blockStateMeta.setBlockState(shulker);
                shulkerItem.setItemMeta(blockStateMeta);
            }
        }
    }
}
