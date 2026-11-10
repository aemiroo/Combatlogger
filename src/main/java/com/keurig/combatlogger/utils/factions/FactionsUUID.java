package com.keurig.combatlogger.utils.factions;

import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Reflective MassiveCraft FactionsUUID adapter. No Factions classes are linked
 * until this adapter is used, allowing the integration to remain optional.
 */
public class FactionsUUID implements FactionsHook {

    @Override
    public void disableFlight(Player player) {
        if (player == null) {
            return;
        }

        try {
            Class<?> playersClass = Class.forName("com.massivecraft.factions.FPlayers");
            Object players = playersClass.getMethod("getInstance").invoke(null);
            Object factionPlayer = playersClass.getMethod("getByPlayer", Player.class).invoke(players, player);
            if (factionPlayer == null) {
                return;
            }

            Class<?> fPlayerClass = factionPlayer.getClass();
            boolean flying = (Boolean) fPlayerClass.getMethod("isFlying").invoke(factionPlayer);
            if (flying) {
                fPlayerClass.getMethod("setFlying", boolean.class, boolean.class)
                        .invoke(factionPlayer, false, false);
            }

            boolean autoFlying = (Boolean) fPlayerClass.getMethod("isAutoFlying").invoke(factionPlayer);
            if (autoFlying) {
                fPlayerClass.getMethod("setAutoFlying", boolean.class).invoke(factionPlayer, false);
            }
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Unable to disable Factions flight", ex);
        }
    }

    @Override
    public List<String> aliases() {
        return readAliases("aliases");
    }

    @Override
    public List<String> subAliases() {
        try {
            Class<?> rootClass = Class.forName("com.massivecraft.factions.cmd.FCmdRoot");
            Object root = rootClass.getMethod("getInstance").invoke(null);
            Field flyField = rootClass.getField("cmdFly");
            Object flyCommand = flyField.get(root);
            Field aliasesField = flyCommand.getClass().getField("aliases");
            Object aliases = aliasesField.get(flyCommand);
            return aliases instanceof List ? new ArrayList<>((List<String>) aliases) : Collections.<String>emptyList();
        } catch (ReflectiveOperationException ex) {
            return Collections.emptyList();
        }
    }

    private List<String> readAliases(String fieldName) {
        try {
            Class<?> rootClass = Class.forName("com.massivecraft.factions.cmd.FCmdRoot");
            Object root = rootClass.getMethod("getInstance").invoke(null);
            Object aliases = rootClass.getField(fieldName).get(root);
            return aliases instanceof List ? new ArrayList<>((List<String>) aliases) : Collections.<String>emptyList();
        } catch (ReflectiveOperationException ex) {
            return Collections.emptyList();
        }
    }
}
