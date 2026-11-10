package com.keurig.combatlogger.punishment.punishments;

import com.keurig.combatlogger.CombatLogger;
import com.keurig.combatlogger.punishment.Punishment;
import com.keurig.combatlogger.utils.Chat;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BanPunishment extends Punishment {

    public BanPunishment() {
        super("BAN", 2);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        BanInfo banInfo = BanInfo.get(player.getUniqueId());
        if (banInfo == null) {
            return;
        }

        if (banInfo.isBanned()) {
            String message = banInfo.getMessage();
            if (message != null) {
                event.setJoinMessage(Chat.color(message));
                player.kickPlayer(Chat.color(message));
            }
        }
    }

    @Override
    public void onQuit(String label) {
        Player player = getPlayer();
        if (player == null || player.hasPermission("combatlogger.admin")) {
            return;
        }

        Map<String, Object> arguments = getArgs();
        if (arguments == null || arguments.get("message") == null || arguments.get("seconds") == null) {
            CombatLogger.getInstance().getLogger().warning("Ban punishment requires both 'message' and 'seconds'.");
            return;
        }

        try {
            int seconds = Integer.parseInt(String.valueOf(arguments.get("seconds")));
            if (seconds <= 0) {
                return;
            }
            BanInfo.create(player.getUniqueId(), String.valueOf(arguments.get("message")),
                    System.currentTimeMillis() + (seconds * 1000L));
        } catch (NumberFormatException ex) {
            CombatLogger.getInstance().getLogger().warning("Invalid ban punishment duration: " + arguments.get("seconds"));
        }
    }
}

class BanInfo {
    private final UUID uuid;
    private final String message;
    private final long time;

    private static final Map<UUID, BanInfo> banned = new HashMap<UUID, BanInfo>();

    private BanInfo(UUID uuid, String message, long time) {
        this.uuid = uuid;
        this.message = message;
        this.time = time;
        banned.put(uuid, this);
    }

    public boolean isBanned() {
        return time > System.currentTimeMillis();
    }

    public String getMessage() {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) {
            return message;
        }

        String formatted = message.replace("%combatlogger_timeformatted%",
                Chat.timeFormat(time - System.currentTimeMillis(), true));
        formatted = formatted.replace("{timeRemaining}",
                Chat.timeFormat(time - System.currentTimeMillis(), true));
        return CombatLogger.getInstance().replaceMsg(player, formatted);
    }

    public static BanInfo create(UUID uuid, String message, long time) {
        return new BanInfo(uuid, message, time);
    }

    public static BanInfo get(UUID uuid) {
        BanInfo info = banned.get(uuid);
        if (info != null && !info.isBanned()) {
            banned.remove(uuid);
            return null;
        }
        return info;
    }
}
