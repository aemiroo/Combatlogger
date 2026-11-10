package com.keurig.combatlogger.handler;

import com.keurig.combatlogger.CombatLogger;
import com.keurig.combatlogger.actionbar.ActionBar;
import com.keurig.combatlogger.api.CombatLoggerAPI;
import com.keurig.combatlogger.event.PlayerEnterCombatEvent;
import com.keurig.combatlogger.event.PlayerLeaveCombatEvent;
import com.keurig.combatlogger.utils.Chat;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import java.util.*;

@Getter
public class CombatPlayer {

    private final CombatLogger plugin;

    private final Map<UUID, Long> combatLogged;
    private final Set<UUID> players;
    private final Map<UUID, Integer> taskActionBar;
    private final Map<UUID, Integer> taskChat;
    private final Map<UUID, Integer> taskCombat;

    public CombatPlayer(CombatLogger plugin) {
        this.plugin = plugin;

        this.combatLogged = new HashMap<>();

        this.taskActionBar = new HashMap<>();
        this.taskChat = new HashMap<>();
        this.taskCombat = new HashMap<>();
        this.players = new HashSet<>();

        addOnlinePlayers();
    }

    private void addOnlinePlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            this.players.add(player.getUniqueId());
        }
    }

    public void addPlayer(Player player) {
        this.players.add(player.getUniqueId());
    }

    public void removePlayer(Player player) {

        this.players.remove(player.getUniqueId());
        stopTasks(player);
        this.combatLogged.remove(player.getUniqueId());
    }


    /**
     * Starts or refreshes combat for a verified player-versus-player hit.
     * Keeping the pair validation here prevents listeners from accidentally
     * tagging a player for damage caused by a non-player or by themselves.
     */
    public void addCombat(Player player, Player opponent) {
        if (player == null || opponent == null
                || player.getUniqueId().equals(opponent.getUniqueId())) {
            return;
        }

        addCombat(player);
        addCombat(opponent);
    }

    public void addCombat(Player player) {
        if (player == null || !player.isOnline() || player.hasPermission("combatlogger.admin"))
            return;

        final int combatTimer = Math.max(1, this.plugin.getConfig().getInt("combat-timer"));

        final boolean useChat = this.plugin.getConfig().getBoolean("chat.use");
        final String combatOnChat = plugin.replaceMsg(player, this.plugin.getConfig().getString("chat.on-message"));

        final boolean useActionBar = this.plugin.getConfig().getBoolean("actionbar.use");
        String combatOnActionBar = plugin.replaceMsg(player, this.plugin.getConfig().getString("actionbar.on-message"));

        String intervalMessage = plugin.getConfig().getString("chat.interval.message");
        List<Integer> intervals = plugin.getConfig().getIntegerList("chat.interval.seconds");


        final boolean disableFlight = plugin.getConfig().getBoolean("on-combat.disable-flight");

        String forceGamemode = plugin.getConfig().getString("on-combat.force-gamemode");

        boolean alreadyTagged = isTagged(player);
        // Always cancel old tasks, including when the timestamp has expired but
        // its delayed cleanup callback has not run yet.
        stopTasks(player);

        if (!alreadyTagged) {
            if (useChat && combatOnChat != null) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', combatOnChat));
            }
            PlayerEnterCombatEvent enterCombatEvent = new PlayerEnterCombatEvent(player);
            Bukkit.getPluginManager().callEvent(enterCombatEvent);
        }

        if (forceGamemode != null && !forceGamemode.trim().isEmpty()) {
            try {
                GameMode gameMode = GameMode.valueOf(forceGamemode.trim().toUpperCase(Locale.ROOT));
                if (!player.getGameMode().equals(gameMode)) {
                    player.setGameMode(gameMode);
                    String message = plugin.getConfig().getString("on-combat.force-gamemode-message");
                    if (message != null) {
                        Chat.message(player, message);
                    }
                }
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("Invalid on-combat.force-gamemode value: " + forceGamemode);
            }
        }

        if (disableFlight) {
            player.setFlying(false);
            player.setAllowFlight(false);

            if (this.plugin.isFactionsEnabled() && CombatLogger.getFactionsHook() != null) {
                try {
                    CombatLogger.getFactionsHook().disableFlight(player);
                } catch (RuntimeException ex) {
                    plugin.getLogger().warning("Unable to disable Factions flight for "
                            + player.getName() + ": " + ex.getMessage());
                }
            }
        }

        if (!intervals.isEmpty() && intervalMessage != null) {
            this.taskChat.put(player.getUniqueId(), Bukkit.getScheduler().scheduleSyncRepeatingTask(this.plugin, () -> {
                String finalIntervalMessage = intervalMessage;

                long timeRemainingMillis = CombatLoggerAPI.timeRemaining(player);

                if (timeRemainingMillis > 0) { // Ensure non-negative time remaining
                    String time = Chat.timeFormat(timeRemainingMillis, false);

                    finalIntervalMessage = finalIntervalMessage.replace("{timeRemaining}", time);
                    finalIntervalMessage = finalIntervalMessage.replace("%combatlogger_timeformatted%", time);
                    finalIntervalMessage = finalIntervalMessage.replace("%combatlogger_time%", time);

                    int timeRemainingSeconds = (int) ((timeRemainingMillis + 999L) / 1000L);

                    if (intervals.contains(timeRemainingSeconds)) {
                        Chat.message(player, finalIntervalMessage);
                    }
                }
            }, 0, 20));
        }


        if (useActionBar && combatOnActionBar != null) {
            int actionBarInterval = Math.max(1, plugin.getConfig().getInt("actionbar.interval", 1));
            long actionBarPeriodTicks = 20L * actionBarInterval;
            this.taskActionBar.put(player.getUniqueId(), Bukkit.getScheduler().scheduleSyncRepeatingTask(this.plugin, () -> {
                if (!player.isOnline() || combatOnActionBar == null) {
                    return;
                }

                long remainingMillis = CombatLoggerAPI.timeRemaining(player);
                String formattedTime = Chat.timeFormat(remainingMillis);
                String finalCombatOnActionBar = combatOnActionBar
                        .replace("{timeRemaining}", formattedTime)
                        .replace("%combatlogger_timeformatted%", formattedTime)
                        .replace("%combatlogger_time%", String.valueOf(remainingMillis));
                ActionBar.sendActionBar(player, ChatColor.translateAlternateColorCodes('&', finalCombatOnActionBar));
            }, 0, actionBarPeriodTicks));
        }

        // Store the current time in milliseconds when a player enters combat,
        // along with the time when they will leave combat based on the combatTimer.
        this.combatLogged.put(player.getUniqueId(), System.currentTimeMillis() + (combatTimer * 1000L));

        // Schedule a task to run after 'combatTimer' seconds to check if the player
        // is still in combat.
        this.taskCombat.put(player.getUniqueId(), Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            // Check if the player's ActionBar task exists and cancel it if so.
            if (taskActionBar.containsKey(player.getUniqueId())) {
                Bukkit.getScheduler().cancelTask(this.taskActionBar.get(player.getUniqueId()));
            }

            if (taskChat.containsKey(player.getUniqueId())) {
                Bukkit.getScheduler().cancelTask(this.taskChat.get(player.getUniqueId()));
            }

            // Check if the player is still marked as in combat.
            if (this.combatLogged.containsKey(player.getUniqueId())) {
                removeCombat(player);
            }

        }, 20L * combatTimer).getTaskId());

    }

    public void removeCombat(Player player) {
        final boolean useChat = this.plugin.getConfig().getBoolean("chat.use");
        final String combatOffChat = plugin.replaceMsg(player, this.plugin.getConfig().getString("chat.off-message"));

        final boolean useActionBar = this.plugin.getConfig().getBoolean("actionbar.use");
        final String combatOffActionBar = plugin.replaceMsg(player, this.plugin.getConfig().getString("actionbar.off-message"));


        stopTasks(player);

        // If configured to do so, send an ActionBar message to the player.
        if (useActionBar && combatOffActionBar != null) {
            ActionBar.sendActionBar(player, ChatColor.translateAlternateColorCodes('&', combatOffActionBar));
        }

        // If configured to do so, send a chat message to the player.
        if (useChat && combatOffChat != null) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', combatOffChat));
        }

        // Call a custom event to signify that the player has left combat.
        PlayerLeaveCombatEvent leaveCombatEvent = new PlayerLeaveCombatEvent(player);
        Bukkit.getPluginManager().callEvent(leaveCombatEvent);

        // Remove the player from the combat logged list.
        this.combatLogged.remove(player.getUniqueId());
    }

    private void stopTasks(Player player) {
        UUID playerId = player.getUniqueId();

        Integer actionBarTask = taskActionBar.remove(playerId);
        if (actionBarTask != null) {
            Bukkit.getScheduler().cancelTask(actionBarTask);
        }

        Integer chatTask = taskChat.remove(playerId);
        if (chatTask != null) {
            Bukkit.getScheduler().cancelTask(chatTask);
        }

        Integer combatTask = taskCombat.remove(playerId);
        if (combatTask != null) {
            Bukkit.getScheduler().cancelTask(combatTask);
        }
    }

    /**
     * Cancels every outstanding task and clears transient state during disable.
     */
    public void shutdown() {
        for (Integer taskId : new ArrayList<Integer>(taskActionBar.values())) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
        for (Integer taskId : new ArrayList<Integer>(taskChat.values())) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
        for (Integer taskId : new ArrayList<Integer>(taskCombat.values())) {
            Bukkit.getScheduler().cancelTask(taskId);
        }

        taskActionBar.clear();
        taskChat.clear();
        taskCombat.clear();
        combatLogged.clear();
        players.clear();
    }

    public boolean isTagged(Player player) {
        if (player == null) {
            return false;
        }
        Long expiresAt = this.combatLogged.get(player.getUniqueId());
        return expiresAt != null && expiresAt.longValue() > System.currentTimeMillis();
    }

    private long combatTimeRemaining(Player player) {
        Long expiresAt = this.combatLogged.get(player.getUniqueId());
        return expiresAt == null ? 0L : Math.max(0L, expiresAt.longValue() - System.currentTimeMillis());
    }
}
