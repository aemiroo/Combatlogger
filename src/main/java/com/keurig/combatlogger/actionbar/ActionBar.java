package com.keurig.combatlogger.actionbar;

import com.keurig.combatlogger.CombatLogger;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public final class ActionBar {

    private ActionBar() {
    }

    public static void sendActionBar(Player player, String message) {
        if (player == null || message == null || !player.isOnline()) {
            return;
        }

        String nmsVersion = CombatLogger.getInstance().getNsmVersion();
        if ("v1_8_R3".equals(nmsVersion)) {
            sendLegacyActionBar(player, message, nmsVersion);
            return;
        }

        sendSpigotActionBar(player, message);
    }

    private static void sendLegacyActionBar(Player player, String message, String version) {
        try {
            Class<?> componentClass = Class.forName(
                    "net.minecraft.server." + version + ".IChatBaseComponent");
            Class<?> packetClass = Class.forName(
                    "net.minecraft.server." + version + ".PacketPlayOutChat");
            Constructor<?> packetConstructor = packetClass.getConstructor(componentClass, Byte.TYPE);

            String json = "{\"text\":\"" + escapeJson(message) + "\"}";
            Object component = componentClass.getDeclaredClasses()[0]
                    .getMethod("a", String.class).invoke(null, json);
            Object packet = packetConstructor.newInstance(component, (byte) 2);
            Object entityPlayer = player.getClass().getMethod("getHandle").invoke(player);
            Object connection = entityPlayer.getClass().getField("playerConnection").get(entityPlayer);
            Class<?> packetInterface = Class.forName("net.minecraft.server." + version + ".Packet");
            connection.getClass().getMethod("sendPacket", packetInterface).invoke(connection, packet);
        } catch (ReflectiveOperationException ex) {
            CombatLogger.getInstance().getLogger().warning(
                    "Unable to send legacy action bar message: " + ex.getMessage());
        }
    }

    private static void sendSpigotActionBar(Player player, String message) {
        try {
            Object spigot = player.getClass().getMethod("spigot").invoke(player);
            Class<?> chatTypeClass = Class.forName("net.md_5.bungee.api.ChatMessageType");
            @SuppressWarnings("unchecked")
            Object actionBarType = Enum.valueOf((Class<Enum>) chatTypeClass, "ACTION_BAR");

            Class<?> textComponentClass = Class.forName("net.md_5.bungee.api.chat.TextComponent");
            Object components = textComponentClass.getMethod("fromLegacyText", String.class)
                    .invoke(null, message);

            for (Method method : spigot.getClass().getMethods()) {
                if ("sendMessage".equals(method.getName())
                        && method.getParameterTypes().length == 2
                        && method.getParameterTypes()[0].equals(chatTypeClass)
                        && method.getParameterTypes()[1].isArray()) {
                    method.invoke(spigot, actionBarType, components);
                    return;
                }
            }

            throw new NoSuchMethodException("Spigot action-bar sendMessage method was not found");
        } catch (ReflectiveOperationException ex) {
            CombatLogger.getInstance().getLogger().warning(
                    "Unable to send action bar message: " + ex.getMessage());
        }
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
