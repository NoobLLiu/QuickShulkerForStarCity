package com.starcity.quickshulker.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;
import net.minecraft.resources.ResourceLocation;
import io.netty.buffer.Unpooled;

/**
 * 客户端数据包发送器
 */
public class ClientPacketSender {

    private static final ResourceLocation CHANNEL = ResourceLocation.fromNamespaceAndPath("quickshulker", "main");

    /**
     * 发送打开潜影盒请求到服务端
     */
    public static void sendOpenRequest(Player player) {
        // 获取玩家主手物品
        ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);

        // 检查是否是潜影盒
        if (!isShulkerBox(mainHand)) {
            // 检查副手
            ItemStack offHand = player.getItemInHand(InteractionHand.OFF_HAND);
            if (isShulkerBox(offHand)) {
                sendOpenPacket(40); // 40 = 副手槽位
                return;
            }
            return;
        }

        // 发送主手槽位
        int slot = player.getInventory().selected;
        sendOpenPacket(slot);
    }

    /**
     * 发送打开指定槽位潜影盒的包
     */
    public static void sendOpenPacket(int slot) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeUtf("open_shulker");
        buf.writeInt(slot);
        ClientPlayNetworking.send(new SimplePayload(CHANNEL, buf));
    }

    /**
     * 发送物品存入潜影盒请求
     */
    public static void sendBundleInsert(int shulkerSlot, int insertSlot) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeUtf("bundle_insert");
        buf.writeInt(shulkerSlot);
        buf.writeInt(insertSlot);
        ClientPlayNetworking.send(new SimplePayload(CHANNEL, buf));
    }

    /**
     * 发送从潜影盒取出物品请求
     */
    public static void sendBundleExtract(int shulkerSlot, int targetSlot) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeUtf("bundle_extract");
        buf.writeInt(shulkerSlot);
        buf.writeInt(targetSlot);
        ClientPlayNetworking.send(new SimplePayload(CHANNEL, buf));
    }

    /**
     * 检查物品是否是潜影盒
     */
    private static boolean isShulkerBox(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        String name = stack.getItem().toString().toLowerCase();
        return name.contains("shulker_box");
    }

    /**
     * 简单的自定义Payload实现
     */
    private record SimplePayload(ResourceLocation id, FriendlyByteBuf data) implements CustomPacketPayload {
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return new Type<>(id);
        }
    }
}
