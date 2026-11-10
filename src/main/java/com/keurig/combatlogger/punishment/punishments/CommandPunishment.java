package com.keurig.combatlogger.punishment.punishments;

import com.keurig.combatlogger.CombatLogger;
import com.keurig.combatlogger.punishment.Punishment;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;

public class CommandPunishment extends Punishment {

    public CommandPunishment() {
        super("COMMAND", 1);
    }

    @Override
    public void onQuit(String label) {
        Player player = getPlayer();
        if (player == null || player.hasPermission("combatlogger.admin")) {
            return;
        }

        Map<String, Object> arguments = getArgs();
        Object configuredCommand = arguments == null ? null : arguments.get("command");
        if (configuredCommand == null) {
            CombatLogger.getInstance().getLogger().warning(
                    "Command punishment was selected but no 'command' argument was configured.");
            return;
        }

        String command = String.valueOf(configuredCommand).replace("%player%", player.getName());
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }
}
