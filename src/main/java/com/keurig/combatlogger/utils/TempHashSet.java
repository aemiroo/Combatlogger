package com.keurig.combatlogger.utils;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A thread-safe set whose entries expire lazily. It creates no Timer threads
 * and never removes a newer entry because an older expiry callback fired late.
 */
public class TempHashSet<E> extends AbstractSet<E> {

    private final long defaultTtlMillis;
    private final Map<E, Long> expirations = new ConcurrentHashMap<E, Long>();

    public TempHashSet(long ttl) {
        this.defaultTtlMillis = Math.max(1L, ttl);
    }

    @Override
    public boolean add(E element) {
        return add(element, defaultTtlMillis);
    }

    public boolean add(E element, long ttlMillis) {
        if (element == null) {
            return false;
        }
        purgeExpired();
        boolean existed = contains(element);
        expirations.put(element, System.currentTimeMillis() + Math.max(1L, ttlMillis));
        return !existed;
    }

    @Override
    public boolean contains(Object element) {
        Long expiresAt = expirations.get(element);
        if (expiresAt == null) {
            return false;
        }
        if (expiresAt.longValue() <= System.currentTimeMillis()) {
            expirations.remove(element, expiresAt);
            return false;
        }
        return true;
    }

    @Override
    public boolean remove(Object element) {
        return expirations.remove(element) != null;
    }

    @Override
    public Iterator<E> iterator() {
        purgeExpired();
        return expirations.keySet().iterator();
    }

    @Override
    public int size() {
        purgeExpired();
        return expirations.size();
    }

    @Override
    public void clear() {
        expirations.clear();
    }

    private void purgeExpired() {
        long now = System.currentTimeMillis();
        for (Map.Entry<E, Long> entry : expirations.entrySet()) {
            if (entry.getValue().longValue() <= now) {
                expirations.remove(entry.getKey(), entry.getValue());
            }
        }
    }
}
