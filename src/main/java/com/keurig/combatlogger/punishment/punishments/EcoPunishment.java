package com.keurig.combatlogger.punishment.punishments;

import com.keurig.combatlogger.punishment.Punishment;
import com.keurig.combatlogger.utils.Chat;
import com.keurig.combatlogger.utils.CombatPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EcoPunishment extends Punishment {

    public static final Map<UUID, String> joinMessages = new HashMap<UUID, String>();

    public EcoPunishment() {
        super("ECO", 2);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(CombatPlugin.getInstance(), new Runnable() {
            @Override
            public void run() {
                String message = joinMessages.remove(player.getUniqueId());
                if (message != null && player.isOnline()) {
                    Chat.message(player, message);
                }
            }
        }, 5L);
    }

    @Override
    public void onQuit(String label) {
        Player player = getPlayer();
        if (player == null || CombatPlugin.getEconomyAPI() == null) {
            return;
        }

        Map<String, Object> arguments = getArgs();
        if (arguments == null || arguments.get("amount") == null) {
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(String.valueOf(arguments.get("amount")));
        } catch (NumberFormatException ex) {
            CombatPlugin.getInstance().getLogger().warning(
                    "Invalid economy punishment amount: " + arguments.get("amount"));
            return;
        }

        if (amount > 0D) {
            CombatPlugin.depositPlayer(player, Math.abs(amount));
        } else if (amount < 0D) {
            CombatPlugin.withdrawPlayer(player, Math.abs(amount));
        }

        Object message = arguments.get("message");
        if (message != null) {
            joinMessages.put(player.getUniqueId(), String.valueOf(message));
        }
    }
}
