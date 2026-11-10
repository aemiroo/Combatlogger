package com.keurig.combatlogger.actionbar;

import com.keurig.combatlogger.CombatLogger;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;

public final class ActionBar {

    private ActionBar() {
    }

    public static void sendActionBar(Player player, String message) {
        if (player == null || message == null || !player.isOnline()) {
            return;
        }

        String nmsVersion = CombatLogger.getInstance().getNsmVersion();
        if ("v1_8_R3".equals(nmsVersion)) {
            try {
                Class<?> componentClass = Class.forName(
                        "net.minecraft.server." + nmsVersion + ".IChatBaseComponent");
                Class<?> packetClass = Class.forName(
                        "net.minecraft.server." + nmsVersion + ".PacketPlayOutChat");
                Constructor<?> packetConstructor = packetClass.getConstructor(componentClass, Byte.TYPE);

                String json = "{\"text\":\"" + escapeJson(message) + "\"}";
                Object component = componentClass.getDeclaredClasses()[0]
                        .getMethod("a", String.class).invoke(null, json);
                Object packet = packetConstructor.newInstance(component, (byte) 2);
                Object entityPlayer = player.getClass().getMethod("getHandle").invoke(player);
                Object connection = entityPlayer.getClass().getField("playerConnection").get(entityPlayer);
                Class<?> packetInterface = Class.forName(
                        "net.minecraft.server." + nmsVersion + ".Packet");
                connection.getClass().getMethod("sendPacket", packetInterface).invoke(connection, packet);
            } catch (ReflectiveOperationException ex) {
                CombatLogger.getInstance().getLogger().warning(
                        "Unable to send legacy action bar message: " + ex.getMessage());
            }
            return;
        }

        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
