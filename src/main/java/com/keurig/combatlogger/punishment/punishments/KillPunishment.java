package com.keurig.combatlogger.punishment.punishments;

import com.keurig.combatlogger.punishment.Punishment;
import org.bukkit.entity.Player;

public class KillPunishment extends Punishment {

    public KillPunishment() {
        super("KILL");
    }

    @Override
    public void onQuit(String label) {
        Player player = getPlayer();
        if (player == null || player.hasPermission("combatlogger.admin")) {
            return;
        }

        player.setHealth(0D);
    }
}
