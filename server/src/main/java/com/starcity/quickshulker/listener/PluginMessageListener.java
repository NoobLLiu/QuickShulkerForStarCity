package com.starcity.quickshulker.listener;

import com.starcity.quickshulker.QuickShulkerPlugin;
import com.starcity.quickshulker.handler.OpenHandler;
import com.starcity.quickshulker.registry.OpenableRegistry;
import org.bukkit.entity.Player;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

/**
 * 监听客户端Mod发送的插件消息（高级功能）
 * 客户端Mod可以通过发送插件消息来触发服务端操作
 */
public class PluginMessageListener implements org.bukkit.plugin.messaging.PluginMessageListener {

    private final QuickShulkerPlugin plugin;
    private final OpenHandler openHandler;
    private final OpenableRegistry registry;

    public PluginMessageListener(QuickShulkerPlugin plugin, OpenHandler openHandler, OpenableRegistry registry) {
        this.plugin = plugin;
        this.openHandler = openHandler;
        this.registry = registry;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(message));
            String action = in.readUTF();

            switch (action) {
                case "open_shulker" -> {
                    // 客户端请求打开潜影盒
                    int slot = in.readInt();
                    if (slot >= 0 && slot < player.getInventory().getSize()) {
                        var item = player.getInventory().getItem(slot);
                        if (registry.isOpenable(item)) {
                            openHandler.openShulker(player, item, slot);
                        }
                    }
                }
                case "bundle_insert" -> {
                    // 客户端请求将物品存入潜影盒
                    int shulkerSlot = in.readInt();
                    int insertSlot = in.readInt();
                    handleBundleInsert(player, shulkerSlot, insertSlot);
                }
                case "bundle_extract" -> {
                    // 客户端请求从潜影盒取出物品
                    int shulkerSlot = in.readInt();
                    int targetSlot = in.readInt();
                    handleBundleExtract(player, shulkerSlot, targetSlot);
                }
            }
        } catch (IOException e) {
            plugin.getLogger().warning("处理客户端插件消息时出错: " + e.getMessage());
        }
    }

    private void handleBundleInsert(Player player, int shulkerSlot, int insertSlot) {
        var shulkerItem = player.getInventory().getItem(shulkerSlot);
        var insertItem = player.getInventory().getItem(insertSlot);

        if (shulkerItem == null || insertItem == null) return;
        if (!registry.isOpenable(shulkerItem)) return;

        // 使用 BundleHandler 处理存入
        com.starcity.quickshulker.handler.BundleHandler.insertIntoShulker(player, shulkerItem, insertItem, shulkerSlot, insertSlot);
    }

    private void handleBundleExtract(Player player, int shulkerSlot, int targetSlot) {
        var shulkerItem = player.getInventory().getItem(shulkerSlot);

        if (shulkerItem == null) return;
        if (!registry.isOpenable(shulkerItem)) return;

        // 使用 BundleHandler 处理取出
        com.starcity.quickshulker.handler.BundleHandler.extractFromShulker(player, shulkerItem, shulkerSlot, targetSlot);
    }
}
