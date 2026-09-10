package com.starcity.quickshulker.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import io.netty.buffer.Unpooled;

/**
 * 客户端数据包发送器
 */
public class ClientPacketSender {

    private static final Identifier CHANNEL = Identifier.of("quickshulker", "main");

    /**
     * 发送打开潜影盒请求到服务端
     */
    public static void sendOpenRequest(PlayerEntity player) {
        // 获取玩家主手物品
        ItemStack mainHand = player.getStackInHand(Hand.MAIN_HAND);

        // 检查是否是潜影盒
        if (!isShulkerBox(mainHand)) {
            // 检查副手
            ItemStack offHand = player.getStackInHand(Hand.OFF_HAND);
            if (isShulkerBox(offHand)) {
                sendOpenPacket(40); // 40 = 副手槽位
                return;
            }
            return;
        }

        // 发送主手槽位
        int slot = player.getInventory().selectedSlot;
        sendOpenPacket(slot);
    }

    /**
     * 发送打开指定槽位潜影盒的包
     */
    public static void sendOpenPacket(int slot) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeString("open_shulker");
        buf.writeInt(slot);
        ClientPlayNetworking.send(new SimplePayload(CHANNEL, buf));
    }

    /**
     * 发送物品存入潜影盒请求
     */
    public static void sendBundleInsert(int shulkerSlot, int insertSlot) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeString("bundle_insert");
        buf.writeInt(shulkerSlot);
        buf.writeInt(insertSlot);
        ClientPlayNetworking.send(new SimplePayload(CHANNEL, buf));
    }

    /**
     * 发送从潜影盒取出物品请求
     */
    public static void sendBundleExtract(int shulkerSlot, int targetSlot) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeString("bundle_extract");
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
    private record SimplePayload(Identifier id, PacketByteBuf data) implements CustomPayload {
        @Override
        public Id<? extends CustomPayload> getId() {
            return new Id<>(id);
        }
    }
}
