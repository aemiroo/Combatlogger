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
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import java.util.ArrayList;

import java.io.File;
import java.util.List;

public class CombatLogger extends CombatPlugin {

    private final List<Listener> registeredListeners = new ArrayList<Listener>();

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

        CombatLoggerCommand command = new CombatLoggerCommand();
        if (getCommand("combatlogger") != null) {
            getCommand("combatlogger").setExecutor(command);
            getCommand("combatlogger").setTabCompleter(command);
        }

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
        unregisterEvents();

        registerListener(new AttackListener());
        registerListener(new JoinListener());
        registerListener(new DeathListener());
        registerListener(new CommandListener(this));

        if (factionsEnabled && factionsHook != null) {
            registerListener(new CommandFlightListener());
        }

        // Check if there are any regions, if not disable section
        List<String> regions = getConfig().getStringList("protected-regions.regions");
        if (!regions.isEmpty()) {
            if (Bukkit.getPluginManager().isPluginEnabled("WorldGuard")) {
                registerListener(new MoveListener());
            } else {
                getLogger().warning("Protected regions are configured, but WorldGuard is not installed. Region movement restrictions are disabled.");
            }
        }
    }

    private void registerListener(Listener listener) {
        Bukkit.getPluginManager().registerEvents(listener, this);
        registeredListeners.add(listener);
    }

    public void unregisterEvents() {
        for (Listener listener : new ArrayList<Listener>(registeredListeners)) {
            HandlerList.unregisterAll(listener);
        }
        registeredListeners.clear();
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
