package com.keurig.combatlogger.utils;

import org.bukkit.Bukkit;

import java.util.HashMap;
import java.util.Map;

/**
 * Main-thread TTL map backed by Bukkit's scheduler. Each put receives a
 * generation token so an older expiry callback cannot remove a newer value,
 * even when the same value object is inserted again.
 */
public class TempHashMap<K, V> extends HashMap<K, V> {

    private final long ttlMillis;
    private final Map<K, Object> generations = new HashMap<K, Object>();

    public TempHashMap(long ttl) {
        this.ttlMillis = Math.max(1L, ttl);
    }

    @Override
    public V put(final K key, final V value) {
        V oldValue = super.put(key, value);
        final Object generation = new Object();
        generations.put(key, generation);

        long ticks = Math.max(1L, (ttlMillis + 49L) / 50L);
        Bukkit.getScheduler().runTaskLater(CombatPlugin.getInstance(), new Runnable() {
            @Override
            public void run() {
                if (generations.get(key) == generation) {
                    generations.remove(key);
                    TempHashMap.super.remove(key);
                }
            }
        }, ticks);
        return oldValue;
    }

    @Override
    public V remove(Object key) {
        generations.remove(key);
        return super.remove(key);
    }

    @Override
    public void clear() {
        generations.clear();
        super.clear();
    }
}
