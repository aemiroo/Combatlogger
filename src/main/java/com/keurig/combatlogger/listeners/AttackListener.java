package com.keurig.combatlogger.listeners;

import com.keurig.combatlogger.CombatLogger;
import com.keurig.combatlogger.handler.CombatPlayer;
import com.keurig.combatlogger.handler.CombatRules;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.List;
import java.util.UUID;

public class AttackListener implements Listener {

    private final CombatLogger plugin;

    public AttackListener() {
        this.plugin = CombatLogger.getInstance();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent event) {
        // CombatLogger is strictly player-versus-player. Entity victims (mobs,
        // animals, NPCs, armor stands, etc.) must never create combat tags.
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player target = (Player) event.getEntity();
        Player attacker = resolvePlayerAttacker(event.getDamager());

        // Environmental damage, mobs, non-player projectile shooters and other
        // non-player sources are intentionally ignored.
        if (attacker == null || !attacker.isOnline() || !target.isOnline()) {
            return;
        }

        // Compare UUIDs rather than object references so self-damage is rejected
        // even if a server implementation supplies different Player wrappers.
        UUID attackerId = attacker.getUniqueId();
        UUID targetId = target.getUniqueId();
        if (!CombatRules.isValidPlayerPair(attackerId, targetId)) {
            return;
        }

        List<String> blacklistedWorlds = plugin.getConfig().getStringList("blacklisted-worlds");
        if (blacklistedWorlds.contains(attacker.getWorld().getName())
                || blacklistedWorlds.contains(target.getWorld().getName())) {
            return;
        }

        CombatPlayer combatPlayer = plugin.getCombatPlayer();
        if (combatPlayer != null) {
            combatPlayer.addCombat(attacker, target);
        }
    }

    private Player resolvePlayerAttacker(Entity damager) {
        if (damager instanceof Player) {
            return (Player) damager;
        }

        if (damager instanceof Projectile) {
            Object shooter = ((Projectile) damager).getShooter();
            if (shooter instanceof Player) {
                return (Player) shooter;
            }
        }

        return null;
    }
}
