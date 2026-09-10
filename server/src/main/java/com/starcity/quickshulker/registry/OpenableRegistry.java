package com.starcity.quickshulker.registry;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * 可打开物品注册表
 */
public class OpenableRegistry {

    private final Map<Material, OpenableData> registry = new HashMap<>();

    /**
     * 注册默认的潜影盒类型
     */
    public void registerDefaults() {
        // 注册所有17种潜影盒
        registerShulkerBox(Material.SHULKER_BOX);
        registerShulkerBox(Material.WHITE_SHULKER_BOX);
        registerShulkerBox(Material.ORANGE_SHULKER_BOX);
        registerShulkerBox(Material.MAGENTA_SHULKER_BOX);
        registerShulkerBox(Material.LIGHT_BLUE_SHULKER_BOX);
        registerShulkerBox(Material.YELLOW_SHULKER_BOX);
        registerShulkerBox(Material.LIME_SHULKER_BOX);
        registerShulkerBox(Material.PINK_SHULKER_BOX);
        registerShulkerBox(Material.GRAY_SHULKER_BOX);
        registerShulkerBox(Material.LIGHT_GRAY_SHULKER_BOX);
        registerShulkerBox(Material.CYAN_SHULKER_BOX);
        registerShulkerBox(Material.PURPLE_SHULKER_BOX);
        registerShulkerBox(Material.BLUE_SHULKER_BOX);
        registerShulkerBox(Material.BROWN_SHULKER_BOX);
        registerShulkerBox(Material.GREEN_SHULKER_BOX);
        registerShulkerBox(Material.RED_SHULKER_BOX);
        registerShulkerBox(Material.BLACK_SHULKER_BOX);
    }

    private void registerShulkerBox(Material material) {
        registry.put(material, new OpenableData(
                material,
                (stack, isOpen) -> isOpen ? Sound.BLOCK_SHULKER_BOX_OPEN : Sound.BLOCK_SHULKER_BOX_CLOSE,
                true  // 潜影盒需要单个物品才能打开
        ));
    }

    /**
     * 注册自定义可打开物品
     */
    public void register(Material material, OpenableData data) {
        registry.put(material, data);
    }

    /**
     * 获取物品的可打开数据
     */
    public OpenableData getOpenableData(ItemStack stack) {
        if (stack == null) return null;
        return registry.get(stack.getType());
    }

    /**
     * 检查物品是否可打开
     */
    public boolean isOpenable(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) return false;
        OpenableData data = registry.get(stack.getType());
        if (data == null) return false;
        // 如果需要单个物品，检查数量
        return !data.isRequiresSingleStack() || stack.getAmount() == 1;
    }

    /**
     * 检查物品是否是已注册的可打开物品类型（不检查数量）
     */
    public boolean isRegistered(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) return false;
        return registry.containsKey(stack.getType());
    }

    /**
     * 获取所有已注册的材料
     */
    public Map<Material, OpenableData> getAll() {
        return registry;
    }
}
