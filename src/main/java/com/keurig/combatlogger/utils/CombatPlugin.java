package com.keurig.combatlogger.utils;

import com.keurig.combatlogger.CombatLogger;
import com.keurig.combatlogger.handler.CombatPlayer;
import com.keurig.combatlogger.api.CombatLoggerAPI;
import com.keurig.combatlogger.punishment.PunishmentManager;
import com.keurig.combatlogger.utils.factions.FactionsHook;
import com.keurig.combatlogger.utils.factions.FactionsManager;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;

public class CombatPlugin extends JavaPlugin {

    @Getter
    protected static CombatLogger instance;

    @Getter
    protected CombatPlayer combatPlayer;

    @Getter
    protected PunishmentManager punishmentManager;

    protected FactionsManager factionsManager;

    @Getter
    protected static FactionsHook factionsHook;

    @Getter
    protected String nsmVersion;

    @Getter
    protected boolean factionsEnabled;

    @Getter
    protected Object placeholderAPIHook;

    // Kept as Object so Vault remains a truly optional runtime dependency.
    @Getter
    protected static Object economyAPI;

    @Getter
    protected Object permissionAPI;

    protected boolean setupEconomy() {
        economyAPI = getVaultProvider("net.milkbowl.vault.economy.Economy");
        return economyAPI != null;
    }

    protected boolean setupPermissions() {
        permissionAPI = getVaultProvider("net.milkbowl.vault.permission.Permission");
        return permissionAPI != null;
    }

    private Object getVaultProvider(String serviceClassName) {
        if (!getServer().getPluginManager().isPluginEnabled("Vault")) {
            return null;
        }

        try {
            Class<?> serviceClass = Class.forName(serviceClassName);
            RegisteredServiceProvider<?> registration =
                    getServer().getServicesManager().getRegistration(serviceClass);
            return registration == null ? null : registration.getProvider();
        } catch (ReflectiveOperationException | LinkageError ex) {
            getLogger().warning("Vault service unavailable (" + serviceClassName + "): " + ex.getMessage());
            return null;
        }
    }

    public static boolean depositPlayer(Player player, double amount) {
        return invokeEconomy("depositPlayer", player, amount);
    }

    public static boolean withdrawPlayer(Player player, double amount) {
        return invokeEconomy("withdrawPlayer", player, amount);
    }

    private static boolean invokeEconomy(String methodName, Player player, double amount) {
        if (economyAPI == null || player == null) {
            return false;
        }

        try {
            for (Method method : economyAPI.getClass().getMethods()) {
                if (methodName.equals(method.getName())
                        && method.getParameterTypes().length == 2
                        && method.getParameterTypes()[0].isAssignableFrom(player.getClass())
                        && (method.getParameterTypes()[1] == double.class
                        || method.getParameterTypes()[1] == Double.class)) {
                    Object response = method.invoke(economyAPI, player, amount);
                    if (response != null) {
                        return (Boolean) response.getClass().getMethod("transactionSuccess").invoke(response);
                    }
                    return true;
                }
            }
        } catch (ReflectiveOperationException ex) {
            getInstance().getLogger().warning("Vault economy operation failed: " + ex.getMessage());
        }
        return false;
    }

    public static boolean hasPermission(Player player, String permission) {
        Object provider = getInstance() == null ? null : getInstance().getPermissionAPI();
        if (provider == null || player == null || permission == null) {
            return false;
        }

        try {
            for (Method method : provider.getClass().getMethods()) {
                if ("has".equals(method.getName())
                        && method.getParameterTypes().length == 2
                        && method.getParameterTypes()[0].isAssignableFrom(player.getClass())
                        && method.getParameterTypes()[1] == String.class) {
                    return (Boolean) method.invoke(provider, player, permission);
                }
            }
        } catch (ReflectiveOperationException ex) {
            getInstance().getLogger().warning("Vault permission check failed: " + ex.getMessage());
        }
        return false;
    }

    /**
     * @deprecated use CombatLoggerAPI.isTagged(Player).
     */
    @Deprecated
    public boolean isTagged(Player player) {
        return CombatLoggerAPI.isTagged(player);
    }
}
