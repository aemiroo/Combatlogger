package com.keurig.combatlogger.utils;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.UUID;
import java.util.logging.Level;

public class Chat {

    public static final TempHashSet<UUID> temp = new TempHashSet<UUID>(1000L);

    public static String color(String message) {
        return message == null ? "" : ChatColor.translateAlternateColorCodes('&', message);
    }

    public static void message(Player player, String... messages) {
        if (player == null || messages == null) {
            return;
        }
        Arrays.stream(messages).filter(java.util.Objects::nonNull)
                .forEach(message -> player.sendMessage(color(message)));
    }

    public static void message(Player player, double seconds, String... messages) {
        if (player == null || messages == null || temp.contains(player.getUniqueId())) {
            return;
        }

        temp.add(player.getUniqueId(), (long) Math.max(1, seconds * 1000));
        Arrays.stream(messages).filter(java.util.Objects::nonNull)
                .forEach(message -> player.sendMessage(color(message)));
    }

    public static void message(CommandSender sender, String... messages) {
        if (sender == null || messages == null) {
            return;
        }
        Arrays.stream(messages).filter(java.util.Objects::nonNull)
                .forEach(message -> sender.sendMessage(color(message)));
    }

    public static void log(String message) {
        CombatPlugin.getInstance().getLogger().log(Level.INFO, color(message));
    }

    public static String timeFormat(long millis) {
        return timeFormat(millis, false);
    }

    public static String timeFormat(long millis, boolean round) {
        millis = Math.max(0L, millis);
        if (millis == 0L) {
            return round ? "1s" : "0s";
        }

        long totalSeconds = round ? (millis + 999L) / 1000L : millis / 1000L;
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;
        if (hours > 0L) {
            return String.format("%dh %dm %ds", hours, minutes, seconds);
        }
        if (minutes > 0L) {
            return String.format("%dm %ds", minutes, seconds);
        }
        if (totalSeconds > 0L) {
            return String.format("%ds", seconds);
        }
        return millis + "ms";
    }
}
