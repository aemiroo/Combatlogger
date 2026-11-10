package com.keurig.combatlogger.listeners;

import com.keurig.combatlogger.CombatLogger;
import com.keurig.combatlogger.api.CombatLoggerAPI;
import com.keurig.combatlogger.utils.Chat;
import com.keurig.combatlogger.utils.factions.FactionsHook;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.List;
import java.util.Locale;

public class CommandFlightListener implements Listener {

    private final CombatLogger plugin = CombatLogger.getInstance();

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!CombatLoggerAPI.isTagged(player) || !plugin.isFactionsEnabled()
                || !plugin.getConfig().getBoolean("on-combat.disable-flight")) {
            return;
        }

        String command = event.getMessage();
        if (command == null || command.length() < 2) {
            return;
        }

        String[] parts = command.substring(1).trim().split("\\s+", 2);
        if (parts.length != 2) {
            return;
        }

        FactionsHook hook = CombatLogger.getFactionsHook();
        if (hook == null) {
            return;
        }

        String root = parts[0].toLowerCase(Locale.ROOT);
        String sub = parts[1].split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
        List<String> aliases = hook.aliases();
        List<String> subAliases = hook.subAliases();

        boolean factionsFlyCommand = containsIgnoreCase(aliases, root)
                && containsIgnoreCase(subAliases, sub);
        if (!factionsFlyCommand) {
            return;
        }

        try {
            hook.disableFlight(player);
        } catch (RuntimeException ex) {
            plugin.getLogger().warning("Factions flight integration failed: " + ex.getMessage());
        }

        Chat.message(player, "&cYou cannot fly during combat!");
        event.setCancelled(true);
    }

    private boolean containsIgnoreCase(List<String> values, String target) {
        for (String value : values) {
            if (value != null && value.equalsIgnoreCase(target)) {
                return true;
            }
        }
        return false;
    }
}
