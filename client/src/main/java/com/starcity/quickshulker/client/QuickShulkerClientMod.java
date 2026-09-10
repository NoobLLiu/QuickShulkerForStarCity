package com.starcity.quickshulker.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class QuickShulkerClientMod implements ClientModInitializer {

    public static final String MOD_ID = "quickshulker_client";
    public static final String CHANNEL = "quickshulker:main";

    private static KeyBinding openKeyBinding;

    @Override
    public void onInitializeClient() {
        // 注册快捷键 (默认 K 键)
        openKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.quickshulker.open",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                "category.quickshulker"
        ));

        // 注册 Tick 事件，检测快捷键
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openKeyBinding.wasPressed()) {
                if (client.player != null) {
                    ClientPacketSender.sendOpenRequest(client.player);
                }
            }
        });

        System.out.println("[QuickShulkerForStarCity-Client] 客户端Mod已加载!");
    }

    public static KeyBinding getOpenKeyBinding() {
        return openKeyBinding;
    }
}
