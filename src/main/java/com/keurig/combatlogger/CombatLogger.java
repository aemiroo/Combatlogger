package com.keurig.combatlogger;

import com.keurig.combatlogger.command.CombatLoggerCommand;
import com.keurig.combatlogger.handler.CombatPlayer;
import com.keurig.combatlogger.listeners.*;
import com.keurig.combatlogger.punishment.PunishmentConfig;
import com.keurig.combatlogger.punishment.PunishmentManager;
import com.keurig.combatlogger.punishment.punishments.EcoPunishment;
import com.keurig.combatlogger.utils.Chat;
import com.keurig.combatlogger.utils.CombatPlugin;
import com.keurig.combatlogger.utils.factions.FactionsManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.List;

public class CombatLogger extends CombatPlugin {

    @Override
    public void onEnable() {
        super.onEnable();
        instance = this;

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            try {
                Class<?> hookClass = Class.forName(
                        "com.keurig.combatlogger.utils.PlaceholderAPIHook");
                Object hook = hookClass.getDeclaredConstructor().newInstance();
                hookClass.getMethod("register").invoke(hook);
                placeholderAPIHook = hook;
            } catch (ReflectiveOperationException | LinkageError ex) {
                getLogger().warning("PlaceholderAPI integration could not be initialized: " + ex.getMessage());
            }
        }

        this.combatPlayer = new CombatPlayer(this);

        // load before punishment manager
        setupPermissions();
        this.punishmentManager = new PunishmentManager();

        this.nsmVersion = Bukkit.getServer().getClass().getPackage().getName();
        this.nsmVersion = this.nsmVersion.substring(this.nsmVersion.lastIndexOf(".") + 1);

        this.factionsManager = new FactionsManager();

        factionsHook = this.factionsManager.getFactionsHook();
        this.factionsEnabled = this.factionsManager.isFactionsEnabled();

        if (setupEconomy()) {
            Chat.log("Ecomony integration has been enabled");
        }

        registerEvents();
        registerConfig();

        getCommand("combatlogger").setExecutor(new CombatLoggerCommand());

    }

    @Override
    public void onDisable() {
        if (this.punishmentManager != null) {
            this.punishmentManager.unregisterPunishments();
        }
        if (this.combatPlayer != null) {
            this.combatPlayer.shutdown();
        }
        EcoPunishment.joinMessages.clear();
        Chat.temp.clear();
    }

    public void registerEvents() {
        Bukkit.getPluginManager().registerEvents(new AttackListener(), this);
        Bukkit.getPluginManager().registerEvents(new JoinListener(), this);
        Bukkit.getPluginManager().registerEvents(new DeathListener(), this);
        Bukkit.getPluginManager().registerEvents(new CommandListener(this), this);

        // Check if there are any regions, if not disable section
        List<String> regions = getConfig().getStringList("protected-regions.regions");
        if (!regions.isEmpty()) {
            if (Bukkit.getPluginManager().isPluginEnabled("WorldGuard")) {
                Bukkit.getPluginManager().registerEvents(new MoveListener(), this);
            } else {
                getLogger().warning("Protected regions are configured, but WorldGuard is not installed. Region movement restrictions are disabled.");
            }
        }
    }

    public void registerConfig() {
        getConfig().options().copyDefaults();
        saveDefaultConfig();
    }

    public String replaceMsg(Player player, String message) {
        if (message == null) {
            return null;
        }
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            try {
                Class<?> placeholderApi = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
                return (String) placeholderApi.getMethod("setPlaceholders", org.bukkit.OfflinePlayer.class, String.class)
                        .invoke(null, player, message);
            } catch (ReflectiveOperationException ex) {
                getLogger().warning("PlaceholderAPI is enabled but could not be called: " + ex.getMessage());
            }
        }

        return message;
    }
}
