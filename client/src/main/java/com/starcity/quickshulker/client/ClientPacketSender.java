package com.starcity.quickshulker.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 客户端数据包发送器
 */
public class ClientPacketSender {

    public static final ResourceLocation CHANNEL_ID =
            ResourceLocation.fromNamespaceAndPath("quickshulker", "main");

    public static final CustomPacketPayload.Type<ShulkerPayload> PACKET_TYPE =
            new CustomPacketPayload.Type<>(CHANNEL_ID);

    /**
     * 发送打开潜影盒请求到服务端
     */
    public static void sendOpenRequest(Player player) {
        ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);

        if (!isShulkerBox(mainHand)) {
            ItemStack offHand = player.getItemInHand(InteractionHand.OFF_HAND);
            if (isShulkerBox(offHand)) {
                sendOpenPacket(40); // 40 = 副手槽位
                return;
            }
            return;
        }

        int slot = player.getInventory().getSelectedSlot();
        sendOpenPacket(slot);
    }

    /**
     * 发送打开指定槽位潜影盒的包
     */
    public static void sendOpenPacket(int slot) {
        ClientPlayNetworking.send(new ShulkerPayload("open_shulker", slot));
    }

    /**
     * 发送物品存入潜影盒请求
     */
    public static void sendBundleInsert(int shulkerSlot, int insertSlot) {
        // 使用 action=2 表示 bundle_insert，数据编码为 shulkerSlot * 1000 + insertSlot
        ClientPlayNetworking.send(new ShulkerPayload("bundle_insert", shulkerSlot, insertSlot));
    }

    /**
     * 发送从潜影盒取出物品请求
     */
    public static void sendBundleExtract(int shulkerSlot, int targetSlot) {
        // 使用 action=3 表示 bundle_extract，数据编码为 shulkerSlot * 1000 + targetSlot
        ClientPlayNetworking.send(new ShulkerPayload("bundle_extract", shulkerSlot, targetSlot));
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
     * 自定义 Payload：存储 action 和 slot 参数，通过 write() 序列化
     */
    public record ShulkerPayload(String action, int slot, int extra) implements CustomPacketPayload {

        public static final StreamCodec<FriendlyByteBuf, ShulkerPayload> STREAM_CODEC =
                StreamCodec.of((buf, payload) -> {
                    buf.writeUtf(payload.action);
                    buf.writeInt(payload.slot);
                    buf.writeInt(payload.extra);
                }, buf -> new ShulkerPayload(buf.readUtf(), buf.readInt(), buf.readInt()));

        public ShulkerPayload(String action, int slot) {
            this(action, slot, 0);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return PACKET_TYPE;
        }

        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeUtf(action);
            buf.writeInt(slot);
            buf.writeInt(extra);
        }
    }
}
