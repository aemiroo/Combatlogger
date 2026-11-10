package com.keurig.combatlogger.utils;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TempHashSetTest {

    @Test
    public void entriesExpireAfterTheirConfiguredTtl() throws InterruptedException {
        TempHashSet<String> values = new TempHashSet<String>(100L);
        values.add("player", 20L);

        assertTrue(values.contains("player"));
        Thread.sleep(60L);
        assertFalse(values.contains("player"));
    }

    @Test
    public void refreshingEntryExtendsItsExpiry() throws InterruptedException {
        TempHashSet<String> values = new TempHashSet<String>(100L);
        values.add("player", 40L);
        Thread.sleep(25L);
        values.add("player", 100L);
        Thread.sleep(30L);

        assertTrue(values.contains("player"));
        Thread.sleep(90L);
        assertFalse(values.contains("player"));
    }
}
