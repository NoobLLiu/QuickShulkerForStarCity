package com.starcity.quickshulker.command;

import com.starcity.quickshulker.QuickShulkerPlugin;
import com.starcity.quickshulker.config.PluginConfig;
import com.starcity.quickshulker.handler.OpenHandler;
import com.starcity.quickshulker.registry.OpenableRegistry;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * QuickShulker 命令处理器
 */
public class QuickShulkerCommand implements CommandExecutor, TabCompleter {

    private final QuickShulkerPlugin plugin;
    private final PluginConfig config;
    private final OpenHandler openHandler;
    private final OpenableRegistry registry;

    private static final List<String> SUB_COMMANDS = Arrays.asList("open", "reload");

    public QuickShulkerCommand(QuickShulkerPlugin plugin, PluginConfig config,
                                OpenHandler openHandler, OpenableRegistry registry) {
        this.plugin = plugin;
        this.config = config;
        this.openHandler = openHandler;
        this.registry = registry;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "open" -> handleOpen(sender);
            case "reload" -> handleReload(sender);
            default -> sendHelp(sender);
        }

        return true;
    }

    private void handleOpen(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c此命令只能由玩家执行");
            return;
        }

        if (!player.hasPermission("quickshulker.open")) {
            player.sendMessage(config.getNoPermissionMessage());
            return;
        }

        if (!config.isCommandOpen()) {
            player.sendMessage("§c命令打开功能已被禁用");
            return;
        }

        if (!openHandler.openShulkerInHand(player)) {
            player.sendMessage(config.getNotShulkerMessage());
        }
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("quickshulker.reload")) {
            sender.sendMessage(config.getNoPermissionMessage());
            return;
        }

        plugin.reload();
        sender.sendMessage(config.getReloadMessage());
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6===== QuickShulkerForStarCity =====");
        sender.sendMessage("§e/qs open §7- 打开手中的潜影盒");
        sender.sendMessage("§e/qs reload §7- 重载配置文件");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            String input = args[0].toLowerCase();
            for (String sub : SUB_COMMANDS) {
                if (sub.startsWith(input)) {
                    completions.add(sub);
                }
            }
            return completions;
        }
        return List.of();
    }
}
