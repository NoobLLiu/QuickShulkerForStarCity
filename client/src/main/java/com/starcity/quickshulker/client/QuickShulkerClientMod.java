package com.starcity.quickshulker.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

public class QuickShulkerClientMod implements ClientModInitializer {

    public static final String MOD_ID = "quickshulker_client";
    public static final String CHANNEL = "quickshulker:main";

    private static final KeyMapping.Category CATEGORY =
            new KeyMapping.Category(Identifier.fromNamespaceAndPath(MOD_ID, "quickshulker"));

    private static KeyMapping openKeyBinding;

    @Override
    public void onInitializeClient() {
        // 注册快捷键 (默认 K 键)
        openKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.quickshulker.open",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                CATEGORY
        ));

        // 注册 Tick 事件，检测快捷键
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openKeyBinding.consumeClick()) {
                if (client.player != null) {
                    ClientPacketSender.sendOpenRequest(client.player);
                }
            }
        });

        System.out.println("[QuickShulkerForStarCity-Client] 客户端Mod已加载!");
    }

    public static KeyMapping getOpenKeyBinding() {
        return openKeyBinding;
    }
}
