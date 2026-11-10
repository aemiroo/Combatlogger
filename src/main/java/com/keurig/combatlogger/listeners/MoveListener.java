package com.keurig.combatlogger.listeners;

import com.keurig.combatlogger.CombatLogger;
import com.keurig.combatlogger.api.CombatLoggerAPI;
import com.keurig.combatlogger.utils.Chat;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class MoveListener implements Listener {

    private final CombatLogger plugin = CombatLogger.getInstance();
    private final Map<String, Object> regionCache = new HashMap<String, Object>();
    private boolean worldGuardWarningLogged;

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!CombatLoggerAPI.isTagged(player)) {
            return;
        }

        Location to = event.getTo();
        if (to == null) {
            return;
        }

        Location from = event.getFrom().clone();
        // Preserve normal movement; only the configured protected-region boundary is restricted.
        from.setYaw(to.getYaw());
        from.setPitch(to.getPitch());
        if (to.getY() < from.getY()) {
            from.setY(to.getY());
        }

        List<String> regions = plugin.getConfig().getStringList("protected-regions.regions");
        for (String regionName : regions) {
            try {
                if (isInsideRegion(player.getWorld(), regionName, to)
                        && !isInsideRegion(player.getWorld(), regionName, event.getFrom())) {
                    event.setTo(from);
                    double messageInterval = plugin.getConfig().getDouble("protected-regions.interval");
                    Chat.message(player, messageInterval > 0D ? messageInterval : 5D,
                            plugin.getConfig().getString("protected-regions.message"));
                    return;
                }
            } catch (ReflectiveOperationException ex) {
                if (!worldGuardWarningLogged) {
                    plugin.getLogger().warning("WorldGuard region check failed for '" + regionName
                            + "'. Check that a compatible WorldGuard version is installed: " + ex.getMessage());
                    worldGuardWarningLogged = true;
                }
                return;
            }
        }

    }

    /**
     * Supports WorldGuard 7 (BlockVector3 / BukkitAdapter) and WorldGuard 6
     * (BlockVector / WorldGuardPlugin). Reflection keeps WorldGuard optional
     * and prevents its classes from being linked on servers without the plugin.
     */
    private boolean isInsideRegion(World world, String regionName, Location location)
            throws ReflectiveOperationException {
        String cacheKey = world.getName() + ":" + regionName.toLowerCase(java.util.Locale.ROOT);
        Object region;
        if (regionCache.containsKey(cacheKey)) {
            region = regionCache.get(cacheKey);
        } else {
            region = getWorldGuard7Region(world, regionName);
            if (region == null) {
                region = getWorldGuard6Region(world, regionName);
            }
            regionCache.put(cacheKey, region);
        }

        if (region == null) {
            return false;
        }

        Class<?> vectorClass;
        try {
            vectorClass = Class.forName("com.sk89q.worldedit.math.BlockVector3");
            Object vector = vectorClass.getMethod("at", int.class, int.class, int.class)
                    .invoke(null, location.getBlockX(), location.getBlockY(), location.getBlockZ());
            return (Boolean) region.getClass().getMethod("contains", vectorClass).invoke(region, vector);
        } catch (ClassNotFoundException legacyWorldEdit) {
            vectorClass = Class.forName("com.sk89q.worldedit.Vector");
        }
        Object vector = vectorClass.getConstructor(double.class, double.class, double.class)
                .newInstance((double) location.getBlockX(), (double) location.getBlockY(),
                        (double) location.getBlockZ());
        return (Boolean) region.getClass().getMethod("contains", vectorClass).invoke(region, vector);
    }

    private Object getWorldGuard7Region(World world, String regionName)
            throws ReflectiveOperationException {
        Class<?> worldGuardClass;
        try {
            worldGuardClass = Class.forName("com.sk89q.worldguard.WorldGuard");
        } catch (ClassNotFoundException ex) {
            return null;
        }

        Object worldGuard = worldGuardClass.getMethod("getInstance").invoke(null);
        Object platform = worldGuardClass.getMethod("getPlatform").invoke(worldGuard);
        Object container = platform.getClass().getMethod("getRegionContainer").invoke(platform);
        Class<?> adapterClass = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");
        Object adaptedWorld = adapterClass.getMethod("adapt", World.class).invoke(null, world);
        Object manager = container.getClass().getMethod("get",
                Class.forName("com.sk89q.worldedit.world.World")).invoke(container, adaptedWorld);
        return manager == null ? null : manager.getClass().getMethod("getRegion", String.class)
                .invoke(manager, regionName);
    }

    private Object getWorldGuard6Region(World world, String regionName)
            throws ReflectiveOperationException {
        Class<?> pluginClass;
        try {
            pluginClass = Class.forName("com.sk89q.worldguard.bukkit.WorldGuardPlugin");
        } catch (ClassNotFoundException ex) {
            return null;
        }

        Object worldGuard = pluginClass.getMethod("inst").invoke(null);
        Object manager = pluginClass.getMethod("getRegionManager", World.class).invoke(worldGuard, world);
        return manager == null ? null : manager.getClass().getMethod("getRegion", String.class)
                .invoke(manager, regionName);
    }
}
