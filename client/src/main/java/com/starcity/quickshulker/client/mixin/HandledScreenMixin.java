package com.starcity.quickshulker.client.mixin;

import com.starcity.quickshulker.client.ClientPacketSender;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin: 拦截背包界面的鼠标点击
 * 当玩家在背包中右键点击潜影盒时，发送打开请求到服务端
 */
@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {

    @Shadow
    protected Slot focusedSlot;

    @Shadow
    protected abstract Slot getSlotAt(double x, double y);

    /**
     * 拦截鼠标点击事件
     * 右键点击玩家背包中的潜影盒时，发送打开请求
     */
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        // 只处理右键点击 (button == 1)
        if (button != 1) return;

        // 检查是否有焦点槽位
        if (this.focusedSlot == null) return;

        // 检查是否在玩家背包区域
        if (!(this.focusedSlot.inventory instanceof PlayerInventory)) return;

        ItemStack stack = this.focusedSlot.getStack();
        if (stack == null || stack.isEmpty()) return;

        // 检查是否是潜影盒
        if (!isShulkerBox(stack)) return;

        // 检查物品数量是否为1（潜影盒需要单个才能打开）
        if (stack.getCount() != 1) return;

        // 发送打开请求到服务端
        ClientPacketSender.sendOpenPacket(this.focusedSlot.getIndex());

        // 取消原始点击事件
        cir.setReturnValue(true);
    }

    private boolean isShulkerBox(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        String name = stack.getItem().toString().toLowerCase();
        return name.contains("shulker_box");
    }
}
