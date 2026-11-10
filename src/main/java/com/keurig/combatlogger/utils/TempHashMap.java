package com.keurig.combatlogger.utils;

import org.bukkit.Bukkit;

import java.util.HashMap;

/**
 * Main-thread TTL map backed by Bukkit's scheduler. Replacing an entry resets
 * its expiry without allowing an older scheduled callback to remove it.
 */
public class TempHashMap<K, V> extends HashMap<K, V> {

    private final long ttlMillis;

    public TempHashMap(long ttl) {
        this.ttlMillis = Math.max(1L, ttl);
    }

    @Override
    public V put(final K key, final V value) {
        V oldValue = super.put(key, value);
        long ticks = Math.max(1L, (ttlMillis + 49L) / 50L);
        Bukkit.getScheduler().runTaskLater(CombatPlugin.getInstance(), new Runnable() {
            @Override
            public void run() {
                if (TempHashMap.super.get(key) == value) {
                    TempHashMap.super.remove(key);
                }
            }
        }, ticks);
        return oldValue;
    }
}
