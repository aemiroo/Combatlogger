package com.keurig.combatlogger.punishment;

import com.keurig.combatlogger.CombatLogger;
import com.keurig.combatlogger.punishment.punishments.*;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.*;

@Getter
public class PunishmentManager {

    private static CombatLogger plugin;
    private final PunishmentConfig config;

    @Getter
    private static List<Punishment> punishments;
    public Map<UUID, Punishment> lastPunishment = new HashMap<UUID, Punishment>();

    public PunishmentManager() {
        plugin = CombatLogger.getInstance();
        punishments = new ArrayList<Punishment>();

        // Register punishment types before loading configuration so YAML
        // entries can resolve their names to actual punishment implementations.
        initializeDefault();
        config = new PunishmentConfig(plugin, new File(plugin.getDataFolder(), "punishments.yml"));
    }

    public void initializeDefault() {
        registerPunishment(new BanPunishment(), plugin);
        registerPunishment(new KillPunishment());
        registerPunishment(new WhitelistPunishment());
        registerPunishment(new CommandPunishment());
        registerPunishment(new EcoPunishment(), plugin);
    }

    public static void registerPunishment(Punishment punishment) {
        punishments.add(punishment);
    }

    public static void registerPunishment(Punishment punishment, Plugin listenerPlugin) {
        Bukkit.getPluginManager().registerEvents(punishment, listenerPlugin);
        punishments.add(punishment);
    }

    public void unregisterPunishments() {
        for (Punishment punishment : new ArrayList<Punishment>(punishments)) {
            HandlerList.unregisterAll(punishment);
        }
        punishments.clear();
        lastPunishment.clear();
    }

    public void reload() {
        unregisterPunishments();
        initializeDefault();
        config.reloadConfig();
    }

    public synchronized void onQuit(Player player) {
        if (player == null) {
            return;
        }

        Set<Punishment> selectedPunishments = config.getPunishments(player);
        Map<Punishment, Map<String, Object>> arguments =
                config.getPunishmentArgs(player, selectedPunishments);

        for (Punishment punishment : selectedPunishments) {
            punishment.setArgs(arguments.get(punishment));
            punishment.setPlayer(player);
            punishment.setPunishmentConfig(config);
            try {
                punishment.onQuit(punishment.getName());
            } catch (RuntimeException ex) {
                plugin.getLogger().severe("Punishment '" + punishment.getName()
                        + "' failed for " + player.getName() + ": " + ex.getMessage());
            } finally {
                // Punishments are shared instances; never retain a quitting
                // Player reference or per-player arguments after execution.
                punishment.setPlayer(null);
                punishment.setArgs(Collections.<String, Object>emptyMap());
                punishment.setPunishmentConfig(null);
            }
        }
    }

    public static Punishment getPunishmentByName(String punishmentName) {
        for (Punishment punishment : punishments) {
            if (punishment.getName().equalsIgnoreCase(punishmentName)) {
                return punishment;
            }
        }
        return null;
    }
}
