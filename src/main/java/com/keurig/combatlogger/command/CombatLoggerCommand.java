package com.keurig.combatlogger.command;

import com.keurig.combatlogger.CombatLogger;
import com.keurig.combatlogger.utils.Chat;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.event.HandlerList;

import java.util.List;

public class CombatLoggerCommand implements CommandExecutor, TabCompleter {


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        CombatLogger plugin = CombatLogger.getInstance();

        if (!sender.hasPermission("combatlogger.command") && !sender.isOp()) {
            Chat.message(sender, "&cYou do not have permission to use this command.");
            return false;
        }

        if (args.length > 0) {
            String type = args[0];

            if (type.equalsIgnoreCase("reload") || type.equalsIgnoreCase("rl")) {
                plugin.reloadConfig();
                plugin.registerConfig();

                HandlerList.unregisterAll(plugin);
                CombatLogger.getInstance().registerEvents();
                plugin.getPunishmentManager().reload();
                Chat.message(sender, "&eReloaded configuration...");
            } else if (type.equalsIgnoreCase("info")) {

                String infoMsg = String.format
                        ("&7CombatLogger Plugin Info" +
                                        "\n&fServer Version: &6%s \n" +
                                        "&fPlugin Version: &6%s",
                                Bukkit.getServer().getVersion(), plugin.getDescription().getVersion());

                Chat.message(sender, infoMsg);

            } else {
                Chat.message(sender, "&cInvalid arguments. Usage: /combatlogger [reload | info]");
            }
        } else {
            Chat.message(sender, "&cInvalid arguments. Usage: /combatlogger [reload | info]");
        }

        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        return java.util.Arrays.asList("info", "reload");
    }
}
