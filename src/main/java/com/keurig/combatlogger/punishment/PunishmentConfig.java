package com.keurig.combatlogger.punishment;

import com.keurig.combatlogger.utils.CombatPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.*;

public class PunishmentConfig {

    private final CombatPlugin plugin;
    public static final Map<String, Set<Punishment>> punishmentMap = new HashMap<String, Set<Punishment>>();
    public static final Set<Punishment> defaultPunishments = new HashSet<Punishment>();
    private FileConfiguration config;
    private final Object permissionAPI;
    private final File file;

    public PunishmentConfig(CombatPlugin plugin, File file) {
        this.plugin = plugin;
        this.permissionAPI = plugin.getPermissionAPI();
        this.file = file;
        loadConfig(file);
        loadDefault();
        loadPermission();
    }

    public Set<Punishment> getPunishments(Player player) {
        Set<Punishment> selected = new LinkedHashSet<Punishment>();
        boolean matchedPermission = false;

        if (permissionAPI != null) {
            for (Map.Entry<String, Set<Punishment>> entry : punishmentMap.entrySet()) {
                if (permissionAPI.has(player, entry.getKey())) {
                    matchedPermission = true;
                    selected.addAll(entry.getValue());
                }
            }
        }

        if (!matchedPermission) {
            selected.addAll(defaultPunishments);
        }
        return selected;
    }

    public Map<String, Object> getDefaultArg(Punishment punishment) {
        ConfigurationSection section = config.getConfigurationSection(
                "default." + punishment.getName().toLowerCase(Locale.ROOT));
        return section == null
                ? new HashMap<String, Object>()
                : new HashMap<String, Object>(section.getValues(false));
    }

    public Map<Punishment, Map<String, Object>> getPunishmentArgs(Player player) {
        Map<Punishment, Map<String, Object>> result =
                new HashMap<Punishment, Map<String, Object>>();

        for (Punishment punishment : getPunishments(player)) {
            Map<String, Object> arguments = getDefaultArg(punishment);
            ConfigurationSection permissions = config.getConfigurationSection("permissions");

            if (permissions != null && permissionAPI != null) {
                for (String profile : permissions.getKeys(false)) {
                    String permission = permissions.getString(profile + ".permission");
                    if (permission == null || !permissionAPI.has(player, permission)) {
                        continue;
                    }

                    ConfigurationSection configured = permissions.getConfigurationSection(
                            profile + ".punishments." + punishment.getName().toLowerCase(Locale.ROOT));
                    if (configured != null) {
                        arguments.putAll(configured.getValues(false));
                        continue;
                    }

                    Object scalar = permissions.get(
                            profile + ".punishments." + punishment.getName().toLowerCase(Locale.ROOT));
                    if (scalar != null) {
                        String argumentName = punishment.getName().toLowerCase(Locale.ROOT);
                        if ("COMMAND".equalsIgnoreCase(punishment.getName())) {
                            argumentName = "command";
                        }
                        arguments.put(argumentName, scalar);
                    }
                }
            }

            result.put(punishment, arguments);
        }

        return result;
    }

    public void loadDefault() {
        defaultPunishments.clear();
        ConfigurationSection section = config.getConfigurationSection("default");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                Punishment punishment = PunishmentManager.getPunishmentByName(key);
                if (punishment != null) {
                    defaultPunishments.add(punishment);
                }
            }
        }

        for (String name : config.getStringList("default")) {
            Punishment punishment = PunishmentManager.getPunishmentByName(name);
            if (punishment != null) {
                defaultPunishments.add(punishment);
            }
        }
    }

    public void loadPermission() {
        punishmentMap.clear();
        ConfigurationSection permissions = config.getConfigurationSection("permissions");
        if (permissions == null) {
            return;
        }

        for (String profile : permissions.getKeys(false)) {
            String permission = permissions.getString(profile + ".permission");
            if (permission == null || permission.trim().isEmpty()) {
                continue;
            }

            Set<Punishment> configuredPunishments = new LinkedHashSet<Punishment>();
            ConfigurationSection section = permissions.getConfigurationSection(profile + ".punishments");
            if (section != null) {
                for (String key : section.getKeys(false)) {
                    Punishment punishment = PunishmentManager.getPunishmentByName(key);
                    if (punishment != null) {
                        configuredPunishments.add(punishment);
                    }
                }
            }

            for (String name : permissions.getStringList(profile + ".punishments")) {
                Punishment punishment = PunishmentManager.getPunishmentByName(name);
                if (punishment != null) {
                    configuredPunishments.add(punishment);
                }
            }

            punishmentMap.put(permission, configuredPunishments);
        }
    }

    public void loadConfig(File file) {
        if (!file.exists()) {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            plugin.saveResource("punishments.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    public void reloadConfig() {
        loadConfig(file);
        loadDefault();
        loadPermission();
    }

    public boolean isDataType(Object object) {
        return object instanceof Number
                || object instanceof String
                || object instanceof int[]
                || object instanceof Integer[]
                || object instanceof String[];
    }
}
