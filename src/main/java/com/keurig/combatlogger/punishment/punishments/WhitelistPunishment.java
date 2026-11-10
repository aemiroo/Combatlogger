package com.keurig.combatlogger.punishment.punishments;

import com.keurig.combatlogger.punishment.Punishment;
import org.bukkit.entity.Player;

public class WhitelistPunishment extends Punishment {

    public WhitelistPunishment() {
        super("WHITELIST");
    }

    @Override
    public void onQuit(String label) {
        Player player = getPlayer();
        if (player == null || player.hasPermission("combatlogger.admin")) {
            return;
        }

        player.setWhitelisted(false);
    }
}
