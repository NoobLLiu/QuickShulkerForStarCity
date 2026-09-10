package com.starcity.quickshulker.registry;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.inventory.ItemStack;

import java.util.function.BiFunction;

/**
 * 可打开物品的数据
 */
public class OpenableData {

    private final Material material;
    private final BiFunction<ItemStack, Boolean, Sound> soundGetter;
    private final boolean requiresSingleStack;

    public OpenableData(Material material,
                        BiFunction<ItemStack, Boolean, Sound> soundGetter,
                        boolean requiresSingleStack) {
        this.material = material;
        this.soundGetter = soundGetter;
        this.requiresSingleStack = requiresSingleStack;
    }

    public Material getMaterial() {
        return material;
    }

    public Sound getSound(ItemStack stack, boolean isOpenSound) {
        if (soundGetter == null) return null;
        return soundGetter.apply(stack, isOpenSound);
    }

    public boolean isRequiresSingleStack() {
        return requiresSingleStack;
    }
}
