package com.keurig.combatlogger.listeners;

import com.keurig.combatlogger.CombatLogger;
import com.keurig.combatlogger.api.CombatLoggerAPI;
import com.keurig.combatlogger.utils.Chat;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.List;
import java.util.Locale;

public class CommandListener implements Listener {

    private final CombatLogger plugin;

    public CommandListener(CombatLogger plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!CombatLoggerAPI.isTagged(player)) {
            return;
        }

        String message = event.getMessage();
        if (message == null || message.length() < 2) {
            return;
        }

        String commandLine = message.substring(1).trim();
        if (commandLine.isEmpty()) {
            return;
        }

        String label = commandLine.split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
        int namespaceSeparator = label.indexOf(':');
        if (namespaceSeparator >= 0 && namespaceSeparator + 1 < label.length()) {
            label = label.substring(namespaceSeparator + 1);
        }

        List<String> blockedCommands = plugin.getConfig().getStringList("blacklisted-commands");
        for (String blocked : blockedCommands) {
            if (blocked == null) {
                continue;
            }
            String normalized = blocked.trim().toLowerCase(Locale.ROOT);
            if (normalized.startsWith("/")) {
                normalized = normalized.substring(1);
            }
            int blockedNamespace = normalized.indexOf(':');
            if (blockedNamespace >= 0 && blockedNamespace + 1 < normalized.length()) {
                normalized = normalized.substring(blockedNamespace + 1);
            }

            if (label.equals(normalized)) {
                String configuredMessage = plugin.getConfig().getString("blacklisted-command-message");
                if (configuredMessage != null) {
                    Chat.message(player, plugin.replaceMsg(player, configuredMessage));
                }
                event.setCancelled(true);
                return;
            }
        }
    }
}
