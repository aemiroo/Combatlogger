package com.keurig.combatlogger.listeners;

import com.keurig.combatlogger.CombatLogger;
import com.keurig.combatlogger.handler.CombatPlayer;
import com.keurig.combatlogger.api.CombatLoggerAPI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class DeathListener implements Listener {

    private final CombatLogger plugin = CombatLogger.getInstance();

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        CombatPlayer combatPlayer = this.plugin.getCombatPlayer();

        // A death ends the victim's combat state regardless of whether the
        // final damage came from another player, a mob, or the environment.
        if (combatPlayer != null && CombatLoggerAPI.isTagged(player)) {
            combatPlayer.removeCombat(player);
        }
    }
}
