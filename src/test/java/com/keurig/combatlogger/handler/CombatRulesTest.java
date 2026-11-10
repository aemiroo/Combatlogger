package com.keurig.combatlogger.handler;

import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CombatRulesTest {

    @Test
    public void rejectsSelfDamageUsingUuidIdentity() {
        UUID playerId = UUID.randomUUID();

        assertFalse(CombatRules.isValidPlayerPair(playerId, playerId));
    }

    @Test
    public void allowsDamageBetweenDifferentPlayers() {
        assertTrue(CombatRules.isValidPlayerPair(UUID.randomUUID(), UUID.randomUUID()));
    }

    @Test
    public void rejectsMissingPlayerIds() {
        assertFalse(CombatRules.isValidPlayerPair(null, UUID.randomUUID()));
        assertFalse(CombatRules.isValidPlayerPair(UUID.randomUUID(), null));
        assertFalse(CombatRules.isValidPlayerPair(null, null));
    }
}
