package com.keurig.combatlogger.utils.factions;

import com.keurig.combatlogger.CombatLogger;
import com.keurig.combatlogger.listeners.CommandFlightListener;
import com.keurig.combatlogger.utils.Chat;
import lombok.Getter;
import org.bukkit.Bukkit;

@Getter
public class FactionsManager {

    private FactionsHook factionsHook;
    private boolean factionsEnabled;

    public FactionsManager() {
        CombatLogger plugin = CombatLogger.getInstance();

        if (!Bukkit.getPluginManager().isPluginEnabled("Factions")) {
            return;
        }

        try {
            // Only enable the adapter for the MassiveCraft Factions API it
            // understands; other plugins named "Factions" remain unaffected.
            Class.forName("com.massivecraft.factions.FPlayers");
            Class.forName("com.massivecraft.factions.cmd.FCmdRoot");
            this.factionsHook = new FactionsUUID();
            Bukkit.getPluginManager().registerEvents(new CommandFlightListener(), plugin);
            this.factionsEnabled = true;
            Chat.log("Enabled MassiveCraft FactionsUUID support");
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ex) {
            this.factionsEnabled = false;
            this.factionsHook = null;
            plugin.getLogger().warning("Factions is installed, but its API is not compatible. Factions-specific flight handling is disabled: " + ex.getMessage());
        }
    }
}
