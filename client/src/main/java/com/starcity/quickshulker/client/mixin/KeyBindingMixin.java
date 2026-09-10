package com.starcity.quickshulker.client.mixin;

import com.starcity.quickshulker.client.QuickShulkerClientMod;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin: 在HUD上显示快捷键提示（可选功能）
 * 当玩家手持潜影盒时，在屏幕上显示按键提示
 */
@Mixin(Gui.class)
public class KeyBindingMixin {

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
        // 此Mixin可以用于在HUD上显示提示
        // 目前留空，后续可以添加"按K打开潜影盒"的提示
    }
}
